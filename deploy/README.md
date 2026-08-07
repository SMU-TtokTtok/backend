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

인프라 주소와 자격증명은 `application-prod.yml` 이 아니라 compose 환경변수로 주입한다
(Spring 우선순위: 환경변수 > 설정 파일). 편의 때문이 아니라, **같은 비밀번호가 두 군데서
쓰이기 때문**이다 — `APP_DB_PASSWORD` 는 `init-db/01-app-user.sh` 가 롤을 만들 때와 앱이
접속할 때 모두 필요하고, `REDIS_PASSWORD` 는 `--requirepass` 와 앱 양쪽, `MINIO_APP_SECRET_KEY`
는 `minio-init` 의 계정 생성과 앱 양쪽에 쓰인다. `.env` 한 곳에만 두면 둘이 어긋날 수 없다.
서비스명(`postgres`, `redis`, `minio`, `smtp`)도 앱 설정이 아니라 compose 토폴로지 사실이라
여기에 둔다.

**그래서 `APPLICATION_PROD_YAML` 시크릿에는 아래 키가 들어 있으면 안 된다.** 남아 있으면
환경변수에 밀려 무시되는 죽은 값이 되고, 환경변수 주입이 누락됐을 때 fallback 으로 동작해
엉뚱한 곳(옛 RDS 등)에 조용히 붙는다. 키가 아예 없어야 Boot 가 즉시 실패한다.
compose 가 넘기는 값:

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

반대로 **환경변수가 덮어쓰는데도 yml 에서 지우면 안 되는 키가 두 개** 있다.

- `cloud.aws.region.static` — MinIO 에 리전 개념은 없지만 `S3Config` 가 `endpointOverride`
  여부와 무관하게 `Region.of(region)` 을 호출한다. `@Value` 에 기본값이 없어 기동 실패한다.
- `firebase.json` — `FIREBASE_CREDENTIALS_LOCATION` 이 덮어쓰지만,
  `@Value("${firebase.credentials-location:classpath:${firebase.json}}")` 에서 Spring 이
  바깥 키를 조회하기 **전에** 플레이스홀더 문자열을 재귀 파싱한다. 기본값을 쓰지 않는
  경우에도 안쪽 `${firebase.json}` 이 먼저 해석되므로, 없으면 기동 실패한다.

`spring.mail.properties.*` 는 반대로 **넣어도 읽히지 않는다.** `EmailConfig` 가
`JavaMailSender` 빈을 직접 만들어서 Boot 의 메일 자동설정이 물러나기 때문이다.
SMTP 속성을 바꾸려면 yml 이 아니라 `EmailConfig` 를 고쳐야 한다.

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
- **`/opt/ttokttok/data` 의 bind mount 가 없으면 낡은 DB 로 조용히 뜬다.** compose 가
  `/opt/ttokttok/data/postgres` 를 직접 참조하는데, 마운트가 없으면 그건 루트 파티션의 빈
  디렉터리다. postgres 는 PGDATA 가 비었으니 `initdb` 를 돌리고 이어서 `00-restore.sql` 로
  **최초 덤프**를 복원한다. 서비스는 멀쩡해 보이지만 덤프 시점 데이터로 운영되고, 진짜
  데이터는 `/home` 에 방치된 채 양쪽이 갈라진다. `setup.sh` 가 마운트를 단언하지만,
  재부팅 후에는 직접 확인하는 습관이 필요하다.

  ```bash
  mountpoint /opt/ttokttok/data    # "is a mountpoint" 여야 한다
  df -h /opt/ttokttok/data         # /dev/nvme0n1p3(/home) 이어야 한다
  ```

  fstab 항목에 `nofail` 은 일부러 넣지 않았다. 넣으면 마운트 실패해도 부팅이 되어 위 상황이
  조용히 발생한다. 지금은 마운트 실패 시 부팅이 멈추므로 즉시 알아챌 수 있다 — 대신
  헤드리스 서버에서는 콘솔 접근이 필요하다. 트레이드오프를 알고 선택한 것이다.

## 남은 리스크

버킷이 익명 GET 으로 열려 있다. CloudFront 공개 배포와 동일한 **현행 동작**이지만,
지원자 제출 서류가 여기에 포함되므로 URL 을 아는 사람은 누구나 열람할 수 있다.
서명 URL 또는 앱 인가를 거치는 다운로드는 프론트엔드 변경이 필요해 별도 이슈로 다룬다.
