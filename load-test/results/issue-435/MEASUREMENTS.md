# #435 메일 발송 3-A 측정 기록

3-A를 작은 단위로 나눠 단위마다 변경 전후를 같은 방법으로 쟀다.

| 단위 | 내용 | 결과 파일 |
|---|---|---|
| U1 | 결과 메일 조회 트랜잭션과 발송 등록 분리 | `before.json`, `after.json` |
| U2 | SMTP 연결·읽기·쓰기 타임아웃 | `smtp-hang-before.json`, `smtp-hang-after.json` |

## U1. 결과 메일 API의 커넥션 점유

### 질문

`mailExecutor`가 가득 찼을 때 결과 메일 API(`POST /api/admin/applies/{clubId}/send-email`)는 DB 커넥션을 발송이 끝날 때까지 쥐고 있는가? 그동안 다른 API는 어떤 영향을 받는가?

### 코드 근거

- `ApplicantAdminService.sendResultMailToApplicants`는 `@Transactional(readOnly = true)`이다. 이 트랜잭션 안에서 `emailService.sendResultMail`을 합격, 불합격 두 번 호출한다.
- `sendResultMail`은 `@Async("mailExecutor")`다. 실행기는 core 2, max 5, 큐 100, `CallerRunsPolicy`다.
- 실행기가 가득 차면 거절된 작업이 호출 스레드에서 동기로 실행된다. 호출 스레드는 트랜잭션을 연 요청 스레드다.

### 한 통에 걸리는 시간: 운영 실측 (2026-11-10, 읽기 전용)

실험의 메일 지연값을 정하려고 운영 Postfix 로그(`docker logs ttokttok-smtp`)를 확인했다. 남아 있는 로그(8월 7일 이후)의 발송 9건에서 `delays=a/b/c/d` 값(초)을 뽑았다. 수신자 주소는 서버 안에서 지우고 숫자만 가져왔다.

| 구간 | 의미 | 실측 |
|---|---|---|
| a | 앱에서 메일을 받아 큐에 쓰기까지 | 0.02~0.03 |
| b | 큐 관리자 대기 | 0~0.06 |
| c | 네이버 연결 (TLS, 인증 포함) | 0.07~0.31 |
| d | 네이버로 전송 | 0.24~0.47 |

- 앱의 mail- 스레드는 a까지만 기다린다. c와 d는 Postfix 안에서 일어나며, 앱은 그동안 기다리지 않는다. 그래서 실험의 한 통 지연은 25ms로 정했다.
- 같은 로그에서 네이버의 거절, 지연, 반송은 0건이었다.
- Postfix 실제 설정은 `default_destination_concurrency_limit = 20`, `default_destination_rate_delay = 0s`, `smtp_connection_cache_on_demand = yes`, `maximal_queue_lifetime = 5d`이다.

### 환경

| 항목 | 값 |
|---|---|
| 장비 | 로컬 PC. Windows 11, i7-1280P(14코어 20스레드), RAM 16GB. 운영 서버가 아니다. |
| 런타임 | Java 17.0.8.1, Spring Boot 3.5.0 |
| 앱 | `@SpringBootTest` 랜덤 포트. 실제 내장 Tomcat에 HTTP로 요청한다. |
| DB | Docker PostgreSQL 17.4 (`ttokttok-tx-hold-pg`), 실험 전용 스키마 `result_mail_tx_hold_experiment` |
| 커넥션 풀 | Hikari 최대 10개, 획득 대기 30초. 운영 기본값과 같다. |
| 메일 실행기 | 실제 `mailExecutor` 빈 그대로 |
| 메일 발송 | 가짜 `JavaMailSender`. 한 통에 25ms 지연. 실제 SMTP로 보내지 않는다. |
| 외부 서비스 | S3, FCM은 mock |
| 데이터 | 동아리 10곳. 곳마다 관리자 1명, 활성 폼 1개, 서류 합격 20명과 불합격 20명. 요청당 40통이다. |
| 부하 | 측정 중 4개 스레드가 `GET /api/notices`를 쉬지 않고 호출한다. |
| 반복 | 조건마다 3회 |

### 조건

- **NORMAL:** 실행기가 비어 있다.
- **SATURATED:** 끝나지 않는 작업으로 실행기를 채운다(실행 중 5, 큐 100). 그 상태에서 API를 부르면 작업 2개가 모두 거절되어 요청 스레드에서 실행된다.
- 동시 요청 수는 1, 5, 10이다. 서로 다른 동아리가 그 수만큼 같은 순간에 요청한다.

### 실행

```powershell
docker start ttokttok-tx-hold-pg
$env:TTOKTTOK_CONCURRENCY_JDBC_URL = 'jdbc:postgresql://127.0.0.1:<port>/ttokttok426'
$env:TTOKTTOK_MAIL_DELAY_MS = '25'
$env:TTOKTTOK_RECIPIENTS_PER_RESULT = '20'
$env:TTOKTTOK_REPETITIONS = '3'
$env:TTOKTTOK_CONCURRENCY = '1,5,10'
$env:TTOKTTOK_EXPERIMENT_OUT = '<repo>\load-test\results\issue-435\before.json'
./gradlew.bat cleanTest test --tests "*ResultMailTxHoldExperimentTest"
```

실험 코드는 `src/test/java/org/project/ttokttok/domain/applicant/service/ResultMailTxHoldExperimentTest.java`이다. 환경 변수가 없으면 건너뛴다.

### 결과 (`before.json`, 2026-11-10 19:50~19:52)

값은 3회 반복의 범위다. 커넥션 점유는 결과 메일 요청이 빌린 커넥션 중 가장 오래 쥔 것이다. 측정 시간대에 Windows 절전 기록은 없다.

| 조건 | 동시 | 응답 시간 | 커넥션 점유 최대 | 요청 스레드에서 보낸 메일 | 공지 API (측정 구간) |
|---|---|---|---|---|---|
| NORMAL | 1 | 0.14~0.19초 | 0.13~0.17초 | 0통 | 오류 0, p95 4~7ms |
| NORMAL | 5 | 0.15~0.20초 | 0.14~0.18초 | 0통 | 오류 0, p95 5~6ms |
| NORMAL | 10 | 0.18~0.29초 | 0.16~0.26초 | 0통 | 오류 0, p95 155~254ms |
| SATURATED | 1 | 1.50~1.56초 | 1.49~1.55초 | 40통 (전부) | 오류 0, p95 3.5~4.8ms |
| SATURATED | 5 | 1.54~1.61초 | 1.55~1.61초 | 200통 (전부) | 오류 0, p95 4.2~4.8ms |
| SATURATED | 10 | 1.56~1.74초 | 1.57~1.74초 | 400통 (전부) | 오류 0, 최대 1.5~1.7초 (커넥션 획득 대기) |

SATURATED 조건의 추가 관찰:

- PostgreSQL `pg_stat_activity`에서 `idle in transaction` 세션이 최대 약 1.4~1.6초 유지되었다. 결과 메일 요청의 readOnly 트랜잭션이 발송 내내 열려 있었다.
- 동시 10에서는 상태 샘플 63~67개 중 29~31개(약 46%)에서 Hikari 커넥션 10개가 모두 사용 중이었다. 공지 API가 커넥션을 최대 약 1.7초 기다렸다.

### 해석

- 실행기가 포화되면 결과 메일 요청 하나가 커넥션 1개를 **발송 전체 시간** 동안 쥔다. 평소에는 0.3초 이내에 반납한다.
- 40통에 약 1.5초가 걸렸다. 40 × 25ms = 1.0초에 메일 생성 같은 통당 오버헤드가 더해졌다(통당 약 12ms로 추정한다).
- 점유 시간은 "수신자 수 × 한 통 시간"에 비례한다. 요청이 커넥션 수만큼 겹치면 메일과 무관한 API가 그 시간만큼 기다린다. 점유가 획득 대기 한도(30초)를 넘으면 그 API는 실패한다.
- 실측 조건에서 30초를 넘으려면 요청당 수신자가 약 800명 이상이어야 한다(30초 ÷ 통당 약 37ms).
- 다만 한 통 시간에는 상한이 없다. SMTP 타임아웃 설정이 없어서, Postfix가 느려지거나 응답하지 않으면 그만큼 길어진다.
- 응답도 발송이 끝날 때까지 나가지 않는다. 이 문제는 커넥션 분리로 해결되지 않으므로 따로 다룬다.

### 변경

`fix/#435`에서 `sendResultMailToApplicants`의 `@Transactional(readOnly = true)`를 없앴다.

- 수신자 조회만 readOnly `TransactionTemplate`으로 감싼다. 동아리, 관리자 확인, 활성 폼, 합격자와 불합격자 이메일을 이 안에서 꺼낸다.
- 트랜잭션이 끝나 커넥션을 반납한 뒤에 `sendResultMail` 두 번을 호출한다.
- 관리자 확인과 전형 상태 조회는 지연 로딩을 쓰므로 트랜잭션 안에 남겼다. 밖으로 넘기는 것은 이메일 문자열 목록뿐이다.

검토한 다른 방법:

| 방법 | 판단 |
|---|---|
| 커밋 후 훅(`afterCommit`, `@TransactionalEventListener(AFTER_COMMIT)`)에서 등록 | 쓰지 않았다. 훅은 커넥션을 반납하기 전에 실행된다. #436 측정에서 점유가 줄지 않았다(2022ms, 트랜잭션 안 2026ms). 리스너에 `@Async`를 붙여도 포화 시에는 CallerRunsPolicy로 훅 안에서 동기 실행된다. |
| `EmailService`에 `Propagation.NOT_SUPPORTED` | 쓰지 않았다. 바깥 트랜잭션을 멈출 뿐이고, 그 커넥션은 계속 묶여 있다. |
| 다른 빈에 `@Transactional` 조회 메서드를 둠 | 이 방법도 동작한다. 다만 같은 클래스에 두면 자기 호출이라 트랜잭션이 적용되지 않는 함정이 있다. 파일 하나 안에서 경계가 보이는 `TransactionTemplate`을 택했다. |

회귀 테스트: `ApplicantAdminServiceTest.sendResultMailToApplicants_registersMailAfterTransactionEnds`. 트랜잭션 시작과 커밋, 조회, 발송 등록의 순서를 기록하고 다음을 확인한다.

- 조회는 읽기 트랜잭션 안에서 일어난다.
- 발송 등록은 커밋 뒤에 일어난다.

변경 전 코드에서는 이 테스트가 실패했다.

### 결과: 변경 후 (`after.json`, 2026-11-10 20:16~20:18)

조건은 변경 전과 같다(한 통 25ms, 요청당 40통, 3회 반복). 측정 시간대에 Windows 절전 기록은 없다.

| 조건 | 동시 | 지표 | 변경 전 | 변경 후 |
|---|---|---|---|---|
| SATURATED | 1 | 커넥션 점유 최대 | 1.49~1.55초 | **0.10~0.16초** |
| SATURATED | 5 | 커넥션 점유 최대 | 1.55~1.61초 | **0.13~0.17초** |
| SATURATED | 10 | 커넥션 점유 최대 | 1.57~1.74초 | **0.12~0.15초** |
| SATURATED | 10 | 커넥션 풀이 가득 찬 샘플 | 29~31 / 63~67 (약 46%) | **2~3 / 68~69 (약 4%)** |
| SATURATED | 10 | 공지 API 최대 지연 | 1.52~1.70초 | **0.12~0.15초** |
| SATURATED | 10 | 공지 API 처리 건수 | 63~100건 | **1,509~1,669건** |
| SATURATED | 1~10 | `idle in transaction` 최대 유지 | 1.34~1.61초 | **0~1ms** |
| SATURATED | 1~10 | 요청 스레드에서 보낸 메일 | 전부 | 전부 (변화 없음) |
| SATURATED | 1~10 | 결과 메일 응답 시간 | 1.50~1.76초 | 1.55~1.86초 (변화 없음) |
| NORMAL | 1~10 | 커넥션 점유 최대 | 0.13~0.26초 | 0.14~0.25초 (변화 없음) |

- 포화 상태에서도 결과 메일 요청은 조회가 끝나면 커넥션을 반납한다. 점유는 평소와 같은 수준(약 0.1~0.2초)이 되었다.
- 동시 10에서 풀이 가득 찬 샘플 2~3개는 요청 10개가 동시에 조회를 시작한 순간이다. NORMAL 동시 10과 같은 수준이다.
- 응답 시간과 요청 스레드 발송은 그대로다. 포화되면 여전히 요청 스레드가 메일을 보내고, 응답은 발송이 끝난 뒤에 나간다. 이 변경은 커넥션 점유만 고친다.

### 한계

- 포화는 끝나지 않는 작업 105개로 **강제로 만든 상태**다. 운영에서 실제로 일어난 기록은 없다. 결과 발표 한 번이 작업 2개이므로, 끝나지 않은 요청이 약 53개 쌓여야 이 상태가 된다.
- 운영 DB 기준 지원자가 있는 폼은 3개, 지원자는 7명이다. 평가를 종료하면 지원자가 지워지므로 과거 규모는 알 수 없다. 지금 규모에서는 포화가 사실상 일어나지 않는다.
- 한 통 시간의 실측값은 운영 Postfix 로그 9건에서 나왔으므로 표본이 작다. 앱 쪽의 TCP 연결과 `EHLO` 시간은 이 값에 들어 있지 않다.
- 단일 JVM, 로컬 PC, 로컬 PostgreSQL에서 측정했다.
- NORMAL 동시 10에서 공지 API p95가 155~254ms로 오른 것은, 요청 10개가 동시에 시작하며 커넥션을 잠깐 모두 쓴 순간 때문이다(샘플 38~39개 중 3~4개에서 풀이 가득 참). 메일 발송과는 관계없다.

## U2. SMTP 타임아웃

### 질문

Postfix가 연결은 받았지만 응답하지 않으면 결과 메일 발송 스레드는 얼마나 묶이는가?

### 코드 근거

- `EmailConfig`가 만드는 `JavaMailSenderImpl`에 `mail.smtp.connectiontimeout`, `mail.smtp.timeout`, `mail.smtp.writetimeout`이 없었다. JavaMail은 이 값이 없으면 끝없이 기다린다.
- `EmailService.sendResultMail`은 수신자마다 한 통씩 순서대로 보낸다. 한 통이 실패하면 예외를 잡아 실패 수를 세고 다음 수신자로 넘어간다.
- mail- 스레드는 평소 2개만 일한다. 둘 다 묶이면 그 뒤의 결과 메일이 모두 큐에서 기다린다.

### 방법

- 테스트 안의 `ServerSocket`이 연결을 받아 두기만 하고 SMTP 인사말(220)을 보내지 않는다. Postfix가 멈춘 상황을 흉내 낸 것이다. 실제 메일은 어디로도 나가지 않는다.
- 발송기는 `EmailConfig.javaMailSender()`로 만든 실제 객체다. 운영과 같은 무인증, 평문 설정이다.
- 측정 대상은 한 통 발송, 그리고 수신자 3명의 결과 메일 묶음(`EmailService.sendResultMail`을 직접 호출)이다. 각각 최대 60초까지 기다린다.
- 실험 코드: `src/test/java/org/project/ttokttok/infrastructure/email/service/SmtpHangExperimentTest.java`

```powershell
$env:TTOKTTOK_SMTP_EXPERIMENT_OUT = '<repo>\load-test\results\issue-435\smtp-hang-after.json'
./gradlew.bat cleanTest test --tests "*SmtpHangExperimentTest"
```

### 변경

`EmailConfig`에 연결 5초, 읽기 10초, 쓰기 10초 타임아웃을 넣었다. 운영 Postfix가 한 통을 받는 데 걸리는 시간은 20~30ms이므로, 이 값은 그 수백 배의 여유다.

회귀 테스트는 두 개다.

- `EmailConfigTest.setsSmtpTimeouts`: 세 값이 설정되는지 확인한다.
- `EmailConfigTest.failsWithinReadTimeoutWhenServerNeverResponds`: 같은 응답 없는 서버로 보내면 9.5~15초 안에 `MailException`으로 끝나는지 확인한다.

### 결과 (2026-11-11, 로컬 PC, Java 17.0.8.1)

| 측정 | 변경 전 (`smtp-hang-before.json`) | 변경 후 (`smtp-hang-after.json`) |
|---|---|---|
| 한 통 발송 | 60초가 지나도 막혀 있음 | 10.1초에 실패 (`SocketTimeoutException`), 스레드 풀림 |
| 결과 메일 묶음 (수신자 3명) | 60초가 지나도 첫 수신자에서 막혀 있음 | 30.1초에 끝남. 수신자마다 10초 뒤 실패를 기록하고 다음으로 넘어감 |

### 해석

- Postfix가 멈춰도 발송 스레드가 영원히 묶이지 않는다. 한 통에 걸리는 시간에 상한(약 10초)이 생겼다.
- 대신 묶음은 "수신자 수 × 10초" 동안 스레드를 잡는다. 수신자가 40명이면 약 400초다. 같은 서버에 대한 첫 실패 뒤에도 나머지 수신자를 계속 시도하기 때문이다.
- 실패한 수신자는 로그에 실패 건수로만 남고 재시도하지 않는다. 누구에게 다시 보낼지 알 수 없다. 재시도와 수신자별 상태는 3-B에서 다룬다.

### 한계

- 읽기 타임아웃만 실제로 재현했다. 연결 타임아웃(응답 없는 IP로 TCP 연결)과 쓰기 타임아웃(본문 전송 중 상대가 읽지 않음)은 로컬에서 안정적으로 재현하기 어렵다. 그래서 값이 설정되는지만 테스트로 확인했다.
- 로컬 루프백 연결에서 쟀다. 운영 Postfix의 실제 장애 모습(응답 지연, 연결 거부 등)은 재현하지 않았다.
