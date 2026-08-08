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

# data/<서비스> 의 소유자는 "그 디렉터리에 실제로 쓰는 컨테이너의 uid" 여야 한다.
# 전부 $RUN_USER 로 통일하면 안 된다. 컨테이너마다 uid 가 다르고, 어긋나면 컨테이너가
# 자기 데이터 디렉터리에 못 쓴다.
#
#   postgres  uid 70      이미지가 강제한다. user: 를 줄 수 없다.
#   redis     compose user: "1001:1003"   (기본값은 uid 999 — #382)
#   minio     compose user: "1001:1003"
#   certbot   root 로 돈다. root 는 권한을 안 타므로 호스트 도구가 읽기 좋은 쪽에 맞춘다.
#
# 이 표는 docker-compose.yml 의 user: 와 반드시 같이 움직여야 한다. 아래 12단계가
# 실제로 대조하므로, 어긋난 채로 이 스크립트가 끝나지는 않는다.
#
# redis 를 $RUN_USER 로 맞춰둔 채 이미지 기본 uid(999)로 돌렸다가 BGSAVE 가 막혀
# 쓰기 전체가 MISCONF 로 거부된 적이 있다. 로그인이 20시간 죽었는데 컨테이너는
# 내내 healthy 였다 — #382.
declare -A DATA_OWNER=(
    [postgres]="70:70"
    [redis]="$RUN_USER:$RUN_GROUP"
    [minio]="$RUN_USER:$RUN_GROUP"
    [certbot]="$RUN_USER:$RUN_GROUP"
    [certbot/conf]="$RUN_USER:$RUN_GROUP"
    [certbot/www]="$RUN_USER:$RUN_GROUP"
)
chown "$RUN_USER:$RUN_GROUP" "$DATA_SRC"
for d in "${!DATA_OWNER[@]}"; do
    # 디렉터리 자체만 chown 한다. -R 을 쓰면 실행 중인 컨테이너의 데이터 파일까지
    # 소유권이 바뀐다 — 6단계 주석 참고.
    chown "${DATA_OWNER[$d]}" "$DATA_SRC/$d"
done

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
# compose 정의가 이번 실행으로 바뀌었는지 기록해둔다. 11단계에서 쓴다.
compose_before="$(sha256sum "$ROOT/app/docker-compose.yml" 2>/dev/null | cut -d' ' -f1 || true)"
install -m 0664 "$SRC/docker-compose.yml"                        "$ROOT/app/docker-compose.yml"
compose_after="$(sha256sum "$ROOT/app/docker-compose.yml" | cut -d' ' -f1)"
install -m 0775 "$SRC/deploy.sh"                                 "$ROOT/app/deploy.sh"
install -m 0664 "$SRC/docker/postfix/Dockerfile"                 "$ROOT/docker/postfix/Dockerfile"
install -m 0775 "$SRC/docker/postfix/entrypoint.sh"              "$ROOT/docker/postfix/entrypoint.sh"
install -m 0775 "$SRC/docker/minio/init.sh"                      "$ROOT/docker/minio/init.sh"
# upstream.conf 는 여기서 다룬다 — 다른 파일과 달리 덮어쓰면 안 된다.
#
# (1) 이건 설정이 아니라 런타임 상태다. 현재 활성 색을 가리키고 deploy.sh 가 갱신한다.
#     레포 쪽 파일은 app-blue 로 고정돼 있어서, 재실행하면 실제 상태와 무관하게
#     blue 로 되돌아간다. 지금이 green 이면 트래픽 없는 색을 가리키게 된다.
#
# (2) install 은 덮어쓰는 게 아니라 파일을 교체해서 inode 가 바뀐다. nginx 는 이 파일을
#     단일 파일 바인드 마운트로 물고 있어, 교체되면 컨테이너는 옛 inode 를 계속 본다.
#     그 뒤 deploy.sh 의 truncate-write 는 컨테이너에 닿지 않는다. 전환 절차는 전부
#     "성공" 으로 끝나고(nginx -t / reload 도 통과한다) 구 색을 정리하는 순간 전 요청이
#     502 가 된다. 실제로 그렇게 서비스가 내려갔다 — 이슈 #376.
#
# .env / state 와 같은 성격이라 같은 방식으로 다룬다.
if [[ -f "$ROOT/config/nginx/upstream.conf" ]]; then
    log "upstream.conf 이미 존재 — 건드리지 않음 (현재 활성 색 상태)"
else
    install -m 0664 "$SRC/config/nginx/upstream.conf"            "$ROOT/config/nginx/upstream.conf"
fi
install -m 0664 "$SRC/config/nginx/conf.d/proxy-common.inc"      "$ROOT/config/nginx/conf.d/proxy-common.inc"
install -m 0664 "$SRC/config/nginx/templates/http-only.conf"     "$ROOT/config/nginx/templates/http-only.conf"
install -m 0664 "$SRC/config/nginx/templates/https.conf"         "$ROOT/config/nginx/templates/https.conf"
install -m 0664 "$SRC/config/nginx/templates/minio-public.inc"   "$ROOT/config/nginx/templates/minio-public.inc"
install -m 0775 "$SRC/bin/nginx-apply.sh"                        "$ROOT/bin/nginx-apply.sh"
install -m 0775 "$SRC/bin/issue-cert.sh"                         "$ROOT/bin/issue-cert.sh"
install -m 0775 "$SRC/bin/check-certs.sh"                        "$ROOT/bin/check-certs.sh"
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
#
# data/ 안쪽은 건드리지 않는다. 거기는 각 컨테이너가 자기 uid 로 소유하는 영역이고,
# 호스트에서 소유권을 강제할 이유가 없다. 반대로 강제하면 실행 중인 서비스가 깨진다 —
# postgres 는 user: 지정 없이 내부 uid 70 으로 도는데, PGDATA 를 1001:1003 으로
# 바꾸는 순간 자기 파일을 못 읽고 PANIC 한다.
#
#   FATAL: could not open file "global/pg_filenode.map": Permission denied
#   PANIC: could not open file "global/pg_control": Permission denied
#
# 이 스크립트는 멱등이라 스택이 뜬 상태에서도 재실행될 수 있으므로 실제로 일어난다.
# postgres 는 엔트리포인트가 root 로 시작하면서 PGDATA 를 다시 chown 해 복구하지만,
# 그건 이 이미지의 우연한 성질이다. redis/minio 에는 그런 복구 경로가 없다.
#
# 마운트 포인트($ROOT/data) 자체는 제외하지 않는다. 그건 /home 쪽 디렉터리 하나이고
# 1단계에서 이미 같은 소유권으로 맞춰둔다.
log "소유권/권한 설정"
find "$ROOT" -path "$ROOT/data/*" -prune -o -exec chown "$RUN_USER:$RUN_GROUP" {} +
find "$ROOT" -path "$ROOT/data/*" -prune -o -type d -exec chmod 2775 {} +
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

# ── 10. 인증서 갱신 체계 ─────────────────────────────────────────────────
# 갱신 자체는 ttokttok-certbot 컨테이너가 12시간마다 시도한다(compose 정의).
# 여기서 거는 것은 나머지 두 고리다.
#
#   04:30  reload — 갱신된 인증서를 nginx 가 실제로 내보내게 한다.
#                   조건 없이 매일 돌린다. reload 는 graceful 이라 비용이 없고,
#                   "갱신됐는데 reload 를 놓쳤다" 를 원천 차단하는 쪽이 싸다.
#   05:00  감시  — 실제 서빙 중인 인증서의 남은 일수를 보고, 임계치 미만이면
#                   메일로 알린다. 갱신 실패는 컨테이너 로그로만 가서 아무도
#                   보지 않는다. 만료 30일 전부터 시도하므로, 알림이 없으면
#                   실패가 30일간 누적된 뒤 조용히 만료된다.
log "인증서 갱신 cron 등록 (reload 04:30, 만료 감시 05:00)"
cat > /etc/cron.d/ttokttok-certs <<EOF
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
30 4 * * * $RUN_USER cd $ROOT/app && docker compose exec -T nginx nginx -s reload >/dev/null 2>&1
0  5 * * * $RUN_USER $ROOT/bin/check-certs.sh >> $ROOT/logs/certs.log 2>&1
EOF
chmod 0644 /etc/cron.d/ttokttok-certs
# 이름이 바뀌었으므로 옛 파일을 지운다. 남겨두면 reload 가 두 번 등록된다.
rm -f /etc/cron.d/ttokttok-nginx-reload

# ── 11. compose 정의 반영 ────────────────────────────────────────────────
# 이 스크립트가 docker-compose.yml 을 서버에 옮기는 유일한 경로다. CI 는
# deploy.sh 만 부르고, deploy.sh 는 이미 서버에 있는 compose 를 읽을 뿐이다.
# 그래서 compose 를 고쳐 머지해도 setup.sh 를 돌리기 전까지는 아무 일도 안 일어난다.
#
# 파일만 갈아끼우고 끝내면 "배치했다" 는 로그만 남고 실행 중인 컨테이너는 옛 정의
# 그대로 돈다. 다음 배포(deploy.sh 의 up -d)까지 반영이 미뤄지는데, 그 사이에
# 재발한 게 #382 였다. 그래서 여기서 바로 재조정한다.
#
# 내용이 안 바뀌었으면 건드리지 않는다. up -d 는 정의가 바뀐 서비스만 재생성하지만,
# 굳이 매번 부를 이유도 없다.
INFRA_SERVICES="${INFRA_SERVICES:-postgres redis minio minio-init smtp nginx certbot}"
compose_up() { sudo -u "$RUN_USER" sh -c "cd '$ROOT/app' && docker compose $*"; }

if [[ -z "$compose_before" ]]; then
    log "compose 최초 배치 — 기동은 아래 '다음 순서' 참고"
elif [[ "$compose_before" == "$compose_after" ]]; then
    log "compose 정의 변경 없음"
elif ! compose_up ps -q 2>/dev/null | grep -q .; then
    log "compose 정의가 바뀌었지만 스택이 떠 있지 않다 — 재조정 생략"
else
    log "compose 정의가 바뀌었다 — 인프라 서비스 재조정"
    # shellcheck disable=SC2086
    compose_up up -d $INFRA_SERVICES
fi

# ── 12. 소유권 검증 ──────────────────────────────────────────────────────
# 1단계의 DATA_OWNER 표가 compose 의 user: 와 어긋나지 않았는지, 표가 아니라
# 실제로 확인한다. 각 컨테이너의 실행 uid 로 자기 데이터 디렉터리에 파일을
# 만들어 본다.
#
# 이 검증이 필요한 이유는 실패가 조용하기 때문이다. redis 를 예로 들면
#   - 컨테이너는 계속 healthy 다. 헬스체크가 PING 이고 PONG 은 정상적으로 온다.
#   - 읽기는 전부 된다. 쓰기만 죽는다.
#   - 소유권이 바뀐 직후에 안 터진다. 이미 열어둔 AOF fd 로 append 는 계속되므로
#     최대 1시간 뒤 첫 자동 BGSAVE 에서야 실패한다.
# postgres 만 PANIC 으로 즉시 티가 났고, 그건 이 이미지의 성질이지 규칙이 아니다.
log "데이터 디렉터리 쓰기 권한 검증"
probe_failed=0
for cid in $(compose_up ps -q 2>/dev/null); do
    name="$(docker inspect -f '{{.Name}}' "$cid" 2>/dev/null | tr -d /)" || continue
    [[ "$(docker inspect -f '{{.State.Running}}' "$cid")" == true ]] || continue
    pid="$(docker inspect -f '{{.State.Pid}}' "$cid")"
    uid="$(awk '/^Uid:/{print $2}' "/proc/$pid/status" 2>/dev/null)" || continue
    gid="$(awk '/^Gid:/{print $2}' "/proc/$pid/status" 2>/dev/null)"

    # 읽기 전용 마운트와 data/ 밖은 검사 대상이 아니다. 경로 필터는 셸에서 한다 —
    # docker inspect 의 템플릿 함수에는 접두사 비교가 없다.
    while read -r src dest; do
        [[ -n "$dest" ]] || continue
        [[ "$src" == "$ROOT/data/"* || "$src" == "$DATA_SRC/"* ]] || continue
        if docker exec -u "$uid:$gid" "$cid" \
             sh -c "touch '$dest/.setup-probe' 2>/dev/null && rm -f '$dest/.setup-probe'" 2>/dev/null
        then
            printf '  \033[32mOK\033[0m      %-14s uid %-10s %s\n' "${name#ttokttok-}" "$uid:$gid" "$dest"
        else
            printf '  \033[31mDENIED\033[0m  %-14s uid %-10s %s\n' "${name#ttokttok-}" "$uid:$gid" "$dest"
            probe_failed=1
        fi
    done < <(docker inspect -f \
        '{{range .Mounts}}{{if and (eq .Type "bind") .RW}}{{.Source}} {{println .Destination}}{{end}}{{end}}' \
        "$cid" 2>/dev/null)
done

if (( probe_failed )); then
    cat >&2 <<EOF

[setup] 컨테이너가 자기 데이터 디렉터리에 쓰지 못한다.

  compose 의 user: 와 1단계 DATA_OWNER 표가 어긋났다는 뜻이다. 둘을 맞춘 뒤
  해당 서비스를 재생성해야 한다. 이대로 두면 서비스는 healthy 인 채로 쓰기만
  실패한다 — redis 는 MISCONF 로 로그인이 죽고, minio 는 업로드가 죽는다.

    cd $ROOT/app && docker compose up -d --force-recreate <서비스>
EOF
    exit 1
fi

log "완료."
echo
echo "다음 순서:"
echo "  1) $ROOT/app/.env 값 채우기 (DOMAIN/FILE_DOMAIN, 비밀번호들, SMTP 릴레이 계정)"
echo "  2) sudo -u $RUN_USER $ROOT/bin/nginx-apply.sh              # HTTP 설정 생성"
echo "  3) cd $ROOT/app && docker compose up -d postgres redis minio minio-init smtp nginx"
echo "     (certbot 갱신 데몬은 5) 의 issue-cert.sh 가 발급 직후 띄운다)"
echo "  4) sudo -u $RUN_USER $ROOT/bin/import-files.sh <resources.tar>  # 기존 S3 파일 적재"
echo "  5) sudo -u $RUN_USER $ROOT/bin/issue-cert.sh <이메일>       # 인증서 발급"
