#!/usr/bin/env bash
#
# /opt/ttokttok 초기 세팅. sudo 로 한 번만 실행한다. 멱등이라 재실행해도 안전.
#
#   sudo <레포>/deploy/setup.sh
#
set -Eeuo pipefail

SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT=/opt/ttokttok
DATA_SRC=/home/ttokttokuser/data
RUN_USER=ttokttokuser
RUN_GROUP=ttokttok
CICD_USER=ttokttok-cicd
RUNNER_SVC=actions.runner.SMU-TtokTtok-backend.ttokttok-cicd.service

log() { printf '\033[36m[setup]\033[0m %s\n' "$*"; }
[[ $EUID -eq 0 ]] || { echo "sudo 로 실행해야 한다" >&2; exit 1; }

# ── 1. 데이터는 /home(160G) 에 둔다 ──────────────────────────────────────
# /opt 은 루트 파티션(46G 중 34G 여유)이라 DB+업로드가 쌓이면 위험하다.
log "데이터 디렉터리: $DATA_SRC"
mkdir -p "$DATA_SRC"/{postgres,redis,minio,certbot/conf,certbot/www}
chown -R "$RUN_USER:$RUN_GROUP" "$DATA_SRC"

# ── 2. /opt/ttokttok 구조 ────────────────────────────────────────────────
log "디렉터리 구조 생성"
mkdir -p "$ROOT"/{app,bin,docker/postfix,docker/minio,config/app,config/nginx/conf.d,config/nginx/templates,init-db,logs,data}

# ── 3. data 를 /home 으로 bind mount (재부팅 후에도 유지) ────────────────
if ! mountpoint -q "$ROOT/data"; then
    log "bind mount: $DATA_SRC → $ROOT/data"
    mount --bind "$DATA_SRC" "$ROOT/data"
fi
if ! grep -qF " $ROOT/data " /etc/fstab; then
    log "fstab 등록"
    printf '%s %s none bind 0 0\n' "$DATA_SRC" "$ROOT/data" >> /etc/fstab
fi

# 마운트를 단언한다. 이게 없으면 스택이 루트 파티션의 빈 디렉터리 위에서 뜨고,
# postgres 는 PGDATA 가 비었으니 initdb → 00-restore.sql 순으로 "최초 덤프"를 복원한다.
# 서비스는 정상으로 보이지만 낡은 데이터로 운영되고, 진짜 데이터는 /home 에 방치된 채
# 양쪽이 갈라진다. 조용히 일어나므로 여기서 막는다.
mountpoint -q "$ROOT/data" \
    || { echo "[setup] $ROOT/data 가 마운트되지 않았다. $DATA_SRC 확인 필요." >&2; exit 1; }

# ── 4. 파일 배치 ─────────────────────────────────────────────────────────
log "설정/스크립트 배치"
install -m 0664 "$SRC/docker-compose.yml"                        "$ROOT/app/docker-compose.yml"
install -m 0775 "$SRC/deploy.sh"                                 "$ROOT/app/deploy.sh"
install -m 0664 "$SRC/docker/postfix/Dockerfile"                 "$ROOT/docker/postfix/Dockerfile"
install -m 0775 "$SRC/docker/postfix/entrypoint.sh"              "$ROOT/docker/postfix/entrypoint.sh"
install -m 0775 "$SRC/docker/minio/init.sh"                      "$ROOT/docker/minio/init.sh"
install -m 0664 "$SRC/config/nginx/upstream.conf"                "$ROOT/config/nginx/upstream.conf"
install -m 0664 "$SRC/config/nginx/conf.d/proxy-common.inc"      "$ROOT/config/nginx/conf.d/proxy-common.inc"
install -m 0664 "$SRC/config/nginx/templates/http-only.conf"     "$ROOT/config/nginx/templates/http-only.conf"
install -m 0664 "$SRC/config/nginx/templates/https.conf"         "$ROOT/config/nginx/templates/https.conf"
install -m 0664 "$SRC/config/nginx/templates/minio-public.inc"   "$ROOT/config/nginx/templates/minio-public.inc"
install -m 0775 "$SRC/bin/nginx-apply.sh"                        "$ROOT/bin/nginx-apply.sh"
install -m 0775 "$SRC/bin/issue-cert.sh"                         "$ROOT/bin/issue-cert.sh"
install -m 0775 "$SRC/bin/backup-db.sh"                          "$ROOT/bin/backup-db.sh"
install -m 0775 "$SRC/bin/import-files.sh"                       "$ROOT/bin/import-files.sh"
install -m 0775 "$SRC/init-db/01-app-user.sh"                    "$ROOT/init-db/01-app-user.sh"

if [[ ! -f "$ROOT/app/.env" ]]; then
    log ".env 생성 (값은 직접 채워야 한다)"
    install -m 0660 "$SRC/.env.example" "$ROOT/app/.env"
else
    log ".env 이미 존재 — 건드리지 않음"
fi
[[ -f "$ROOT/app/state" ]] || { echo none > "$ROOT/app/state"; chmod 0664 "$ROOT/app/state"; }

# ── 5. 덤프를 초기화 스크립트 위치로 ─────────────────────────────────────
if [[ -f "$ROOT/init-db/ttokttok-backup.sql" && ! -f "$ROOT/init-db/00-restore.sql" ]]; then
    log "덤프 → 00-restore.sql (알파벳 순서로 01-app-user.sh 보다 먼저 실행되게)"
    mv "$ROOT/init-db/ttokttok-backup.sql" "$ROOT/init-db/00-restore.sql"
fi

# ── 6. 소유권/권한 ───────────────────────────────────────────────────────
# setgid(2775): ttokttok-cicd 가 만든 파일도 ttokttok 그룹을 상속 →
# ttokttokuser 와 파일을 주고받을 수 있다.
log "소유권/권한 설정"
chown -R "$RUN_USER:$RUN_GROUP" "$ROOT"
find "$ROOT" -type d -not -path "$ROOT/data/*" -exec chmod 2775 {} +
chmod 0660 "$ROOT/app/.env"
chmod 0770 "$ROOT/config/app"     # 시크릿(application-prod.yml, firebase.json)

# ── 7. docker 권한 ───────────────────────────────────────────────────────
# 두 사용자 다 필요하다.
#   - $CICD_USER: 러너가 이미지 빌드와 deploy.sh 를 돌린다.
#   - $RUN_USER : 운영 주체다. bin/ 아래 스크립트가 전부 docker 를 호출한다 —
#     import-files.sh(docker run), issue-cert.sh(compose run), nginx-apply.sh
#     (compose exec ... -s reload), backup-db.sh(compose exec pg_dump).
#     뒤의 둘은 cron 으로 도는 무인 경로라, 빠뜨리면 백업과 인증서 갱신이
#     아무 신호 없이 계속 실패한다.
if ! id -nG "$RUN_USER" | tr ' ' '\n' | grep -qx docker; then
    log "$RUN_USER 를 docker 그룹에 추가 (bin/ 스크립트·백업 cron 에 필수)"
    usermod -aG docker "$RUN_USER"
    # 러너처럼 재시작할 대상이 없다. $RUN_USER 는 상주 세션이 없고
    # sudo -u 와 cron 이 실행 시점에 그룹을 새로 계산하므로 바로 반영된다.
else
    log "$RUN_USER 는 이미 docker 그룹 소속"
fi

if ! id -nG "$CICD_USER" | tr ' ' '\n' | grep -qx docker; then
    log "$CICD_USER 를 docker 그룹에 추가 (배포에 필수)"
    usermod -aG docker "$CICD_USER"
    log "러너 재시작 (그룹 변경 반영)"
    systemctl restart "$RUNNER_SVC"
else
    log "$CICD_USER 는 이미 docker 그룹 소속"
fi

# ── 8. 방화벽 ────────────────────────────────────────────────────────────
# 주의: ufw 는 Docker 가 삽입한 iptables 규칙보다 나중에 평가되므로 publish 된
# 포트를 막지 못한다. 그래서 compose 에서 인프라 포트를 전부 127.0.0.1 에 묶는다.
if command -v ufw >/dev/null; then
    log "ufw 규칙: 22, 80, 443"
    ufw allow 22/tcp  >/dev/null
    ufw allow 80/tcp  >/dev/null
    ufw allow 443/tcp >/dev/null
    ufw --force enable >/dev/null
    ufw status numbered | head -20
fi

# ── 9. 일일 DB 백업 ──────────────────────────────────────────────────────
log "일일 백업 cron 등록 (매일 04:00)"
cat > /etc/cron.d/ttokttok-backup <<EOF
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
0 4 * * * $RUN_USER $ROOT/bin/backup-db.sh >> $ROOT/logs/backup.log 2>&1
EOF
chmod 0644 /etc/cron.d/ttokttok-backup

# ── 10. certbot 갱신 후 nginx reload ─────────────────────────────────────
cat > /etc/cron.d/ttokttok-nginx-reload <<EOF
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
30 4 * * * $RUN_USER cd $ROOT/app && docker compose exec -T nginx nginx -s reload >/dev/null 2>&1
EOF
chmod 0644 /etc/cron.d/ttokttok-nginx-reload

log "완료."
echo
echo "다음 순서:"
echo "  1) $ROOT/app/.env 값 채우기 (DOMAIN/FILE_DOMAIN, 비밀번호들, SMTP 릴레이 계정)"
echo "  2) sudo -u $RUN_USER $ROOT/bin/nginx-apply.sh              # HTTP 설정 생성"
echo "  3) cd $ROOT/app && docker compose up -d postgres redis minio minio-init smtp nginx"
echo "  4) sudo -u $RUN_USER $ROOT/bin/import-files.sh <resources.tar>  # 기존 S3 파일 적재"
echo "  5) sudo -u $RUN_USER $ROOT/bin/issue-cert.sh <이메일>       # 인증서 발급"
