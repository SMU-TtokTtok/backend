# 자체 서버 운영 인프라

EB → 자체 우분투 서버(`/opt/ttokttok`) 이관용 인프라 코드. 이슈 #354.

## 구성

| 경로 | 설명 |
|---|---|
| `setup.sh` | sudo 1회 실행. 디렉터리·권한·bind mount·docker 그룹·ufw·cron 세팅 |
| `docker-compose.yml` | app(blue/green) + postgres + redis + minio + postfix + nginx + certbot |
| `deploy.sh` | 블루-그린 무중단 배포 진입점 |
| `.env.example` | 비밀값 템플릿 (실제 `.env` 는 서버에만, gitignore 대상) |
| `docker/postfix/` | 릴레이 전용 Postfix 이미지 |
| `docker/minio/init.sh` | 버킷 생성 + 익명 읽기 정책 + 앱 전용 계정 |
| `config/nginx/` | 서버 블록 템플릿(HTTP/HTTPS) + 공용 프록시 설정 + upstream.conf |
| `bin/nginx-apply.sh` | 도메인 치환 후 nginx 설정 적용 (`--https` 로 HTTPS 전환) |
| `bin/issue-cert.sh` | Let's Encrypt 최초 발급 (API/파일 도메인 각각, 사전 도달성 점검 포함) |
| `bin/import-files.sh` | 기존 S3 백업(`resources.tar`)을 MinIO 버킷에 적재 |
| `bin/backup-db.sh` | 일일 pg_dump + 주간 파일 백업 |
| `test/bluegreen-test.sh` | 앱 없이 배포·프록시 로직 검증 |

앱 이미지는 레포 루트의 `Dockerfile`(멀티스테이지)이 담당한다.

## 왜 `/opt/ttokttok` 인가

배포는 이 서버에 등록된 self-hosted runner 가 수행하고, `actions/checkout` 은
매 실행마다 워크스페이스를 정리한다. compose 파일·`.env`·`data/` 가 레포 체크아웃
안에 있으면 배포 한 번에 DB 데이터와 시크릿이 사라진다. 그래서 `setup.sh` 가
이 디렉터리의 내용을 `/opt/ttokttok` 아래로 설치하고, 운영 스택은 거기서 돈다.

## 설치

```bash
sudo <레포>/deploy/setup.sh
vi /opt/ttokttok/app/.env                        # 도메인, 비밀번호, SMTP 릴레이 계정
sudo -u ttokttokuser /opt/ttokttok/bin/nginx-apply.sh
cd /opt/ttokttok/app && docker compose up -d postgres redis minio minio-init smtp nginx
sudo -u ttokttokuser /opt/ttokttok/bin/import-files.sh <resources.tar>
sudo -u ttokttokuser /opt/ttokttok/bin/issue-cert.sh <이메일>
```

`postgres` 최초 기동 시 `init-db/00-restore.sql`(백업 덤프)이 자동 복원된다.
`db/migration` 의 첫 스크립트가 `ALTER TABLE` 로 시작해서 Flyway 만으로는
스키마를 만들 수 없기 때문에, 덤프 복원이 유일한 경로다.

## 운영

```bash
# 수동 배포 (CI 와 동일 경로)
/opt/ttokttok/app/deploy.sh <이미지태그>

# 현재 활성 색
cat /opt/ttokttok/app/state

# 이전 버전으로 되돌리기 — 그 태그로 다시 배포하면 반대 색에 뜬다
/opt/ttokttok/app/deploy.sh <이전태그>

# 로그
cd /opt/ttokttok/app && docker compose logs -f app-$(cat ./state)

# MinIO 콘솔 / psql (외부 노출 없음. SSH 터널로만)
ssh -L 19001:127.0.0.1:19001 -L 15432:127.0.0.1:15432 <서버>
```

## 앱 설정

인프라 주소와 자격증명은 compose 의 환경변수가 `application-prod.yml` 을 덮어쓴다
(Spring 우선순위: 환경변수 > 설정 파일). 덕분에 `APPLICATION_PROD_YAML` 시크릿을
거의 손대지 않아도 된다. compose 가 넘기는 값:

`SPRING_DATASOURCE_*`, `SPRING_DATA_REDIS_*`, `SPRING_MAIL_HOST/PORT`,
`CLOUD_AWS_S3_ENDPOINT/BUCKET`, `CLOUD_AWS_CREDENTIALS_*`, `FILE_CLOUD_URL`,
`FIREBASE_CREDENTIALS_LOCATION`

`application-prod.yml` 쪽에서 확인이 필요한 것:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate      # 블루/그린이 전환 구간에 같은 DB 를 본다
server:
  shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 25s
```

`EmailConfig` 가 `mail.smtp.auth=true` / `starttls=true` 를 고정으로 켜지만,
Postfix 는 `mynetworks` 안에서 오는 요청을 인증 없이 받으므로 사용자/비밀번호는
빈 값이어도 된다. 다만 프로퍼티 자체는 존재해야 `@Value` 주입이 실패하지 않는다.

## 설계상 주의점

- **`FILE_CLOUD_URL` 은 절대 바꾸지 않는다.** 이 값으로 만들어진 공개 URL 이 DB 에
  그대로 저장돼 있고, 단독 컬럼(`clubs.profile_img`, `club_boards.thumbnail_url`)뿐 아니라
  `clubs.content` 마크다운 본문, 지원서 답변 JSON, `temp_applicant.temp_data` jsonb
  **내부 문자열**에도 박혀 있다. 도메인을 유지하는 대가로 DB 를 한 줄도 안 건드린다.
- **`POSTGRES_USER` 는 `postgres` 여야 한다.** 백업 덤프의 객체 소유자가 `postgres` 라,
  다른 이름으로 초기화하면 `ALTER ... OWNER TO postgres` 가 "role does not exist" 로 실패한다.
- **헬스 엔드포인트는 `/health` 다.** 이 앱에는 actuator 의존성이 없어서
  `/actuator/health` 는 404 다. `/health` 는 `SecurityWhiteList` 에 등록된 공개 경로다.
- **`upstream.conf` 는 단일 파일 바인드 마운트**다. `sed -i` 처럼 inode 를 바꾸는 방식으로
  고치면 컨테이너가 옛 파일을 계속 본다. `deploy.sh` 는 truncate-write 를 쓴다.
- **`proxy_pass` 에 변수를 쓴다.** 그래야 nginx 기동 시점에 대상 색 컨테이너가 없어도
  "host not found in upstream" 으로 죽지 않는다. 블루-그린이 nginx 재기동 없이 되는 이유.
- **`minio-public.inc` 는 `set` 이 `rewrite` 보다 먼저 와야 한다.** 둘 다 rewrite 모듈
  지시자이고 `rewrite ... break` 가 그 단계를 끝내므로, 순서가 바뀌면 변수가 비어 500 이 난다.
- **버킷 정책에 `mc anonymous set download` 를 쓰지 않는다.** 그 명령은 `s3:ListBucket` 까지
  열어서 `https://<파일도메인>/?list-type=2` 로 전체 오브젝트 키가 노출된다. 지원자 서류
  경로에 이메일이 들어 있어(`applicant/<이메일>/…`) 그대로 개인정보 유출이다.
  `s3:GetObject` 만 주는 정책을 직접 적용한다.
- **블루/그린이 잠시 같은 DB 를 본다.** 스키마 변경은 expand-contract 로 해야 한다.
- **Postfix 는 릴레이 전용**이다. 이 서버 IP 는 PTR 없음 + Spamhaus PBL 등재라 직접 발송이
  불가능하다.
- **MinIO 는 `user: "1001:1003"` 으로 고정**한다. 기본값이 root 라 바인드 마운트에
  root 소유 파일을 만들고, 그러면 백업 cron 이 `ttokttokuser` 로 읽지 못한다.
- **ufw 는 Docker 가 publish 한 포트를 막지 못한다.** iptables 평가 순서 때문이다.
  그래서 인프라 포트를 전부 `127.0.0.1:` 에 묶는다.

## 남은 리스크

버킷이 익명 GET 으로 열려 있다. CloudFront 공개 배포와 동일한 **현행 동작**이지만,
지원자 제출 서류가 여기에 포함되므로 URL 을 아는 사람은 누구나 열람할 수 있다.
서명 URL 또는 앱 인가를 거치는 다운로드는 프론트엔드 변경이 필요해 별도 이슈로 다룬다.
