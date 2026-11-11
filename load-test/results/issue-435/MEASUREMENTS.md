# #435 메일 발송 3-A 측정 기록

3-A를 작은 단위로 나눠 단위마다 변경 전후를 같은 방법으로 쟀다.

| 단위 | 내용 | 결과 파일 |
|---|---|---|
| U1 | 결과 메일 조회 트랜잭션과 발송 등록 분리 | `before.json`, `after.json` |
| U2 | SMTP 연결·읽기·쓰기 타임아웃 | `smtp-hang-before.json`, `smtp-hang-after.json` |
| U3 | 배포 종료 때 사라지는 메일 작업 | `shutdown-before.json`, `shutdown-before-run1.json`, `shutdown-alt-wait8.json`, `shutdown-after.json` |
| U4 | FCM 알림 실행기 확인과 분리 | `fcm-executor-before.json`, `fcm-executor-after.json` |
| U5 | 메일 실행기가 넘칠 때의 정책 결정 | (U1~U4 결과로 판단) |

#460에서 `mailExecutor`와 실험 테스트(`MailExecutorSaturationTest`, `MailExecutorShutdownExperimentTest`, `ResultMailTxHoldExperimentTest`)를 지웠으므로, 이 문서의 재현 명령은 #435 커밋(`e82157f`)에서만 돈다.

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

## U3. 배포 종료 때 사라지는 메일 작업

### 질문

결과 메일을 보내는 도중 배포로 이전 컨테이너가 내려가면 몇 통이 나가고 몇 통이 사라지는가? 종료 대기 설정으로 줄일 수 있는가?

### 코드 근거

- `deploy.sh`: 새 버전으로 전환한 뒤 30초 드레인, 그다음 `docker compose stop`이다. `stop_grace_period` 설정이 없어 종료 신호 뒤 10초에 강제 종료된다.
- `mailExecutor`에는 종료 대기 설정이 없다(`waitForTasksToCompleteOnShutdown=false`). Spring 6.2는 이 경우 컨텍스트 종료가 시작될 때 실행기에 먼저 종료 신호를 보낸다. 정리 단계의 대기 시간이 끝나면 큐에 남은 작업을 버리고 실행 중인 작업에 인터럽트를 건다(`ExecutorConfigurationSupport`).
- 큐는 메모리에만 있다. 버린 작업은 다시 실행되지 않는다. 지금까지는 몇 건이 버려졌는지도 기록하지 않았다.

### 방법

- 실제 `AsyncConfig`와 `EmailService`(@Async 프록시)로 작은 스프링 컨텍스트를 띄운다.
- 가짜 SMTP는 한 통에 25ms(운영 실측)가 걸린다. 실제 소켓처럼 스레드 인터럽트에 반응하지 않는다.
- 결과 메일 묶음(수신자 100명)을 4개(400통, 가벼움) 또는 10개(1,000통, 무거움) 넣는다. 0.3초 뒤 컨텍스트 종료를 시작한다.
- 종료 시작 0.2초 뒤 수신자 10명 묶음을 하나 더 넣는다. 그레이스풀 셧다운 중 처리되던 요청을 흉내 낸 것이다.
- 배포의 강제 종료를 반영해, **종료 시작 후 10초 안에 나간 메일만 "발송"으로 센다.** 나머지는 실제 배포에서는 사라진다.
- 조건마다 3회 반복했다. 실험 코드: `src/test/java/org/project/ttokttok/global/config/MailExecutorShutdownExperimentTest.java`

```powershell
$env:TTOKTTOK_SHUTDOWN_EXPERIMENT_OUT = '<repo>\load-test\results\issue-435\shutdown-after.json'
./gradlew.bat cleanTest test --tests "*MailExecutorShutdownExperimentTest"
```

### 결과 (2026-11-11, 로컬 PC, 3회 반복의 범위)

| 설정 | 작업량 | 10초 안에 발송 | 실제 배포라면 유실 | 한 번도 시작하지 못한 메일 | 종료 직후 들어온 10통 |
|---|---|---|---|---|---|
| 변경 전 (`shutdown-before.json`) | 400통 | 410 (전부) | 0 | 0 | 발송 |
| 변경 전 | 1,000통 | 567~576 | 434~443 | 410 | 버려짐 |
| 대안: 종료 대기 8초 (`shutdown-alt-wait8.json`) | 400통 | 400 | **10** | 10 | **버려짐** |
| 대안: 종료 대기 8초 | 1,000통 | 446~596 | 414~564 | 10 | 버려짐 |
| 변경 후 (`shutdown-after.json`) | 400통 | 410 (전부) | 0 | 0 | 발송 |
| 변경 후 | 1,000통 | 574~580 | 430~436 | 410 | 버려짐 |

- 무거운 작업량에서 종료 시점 상태는 매번 같았다(실행 중 2개, 대기 8개).
- "10초 안에 발송"은 PC 부하에 따라 흔들린다. 같은 변경 전 설정을 먼저 돌린 1회차(`shutdown-before-run1.json`)는 445~447이었다. 그래서 설정 비교는 흔들리지 않는 두 값으로 했다. "한 번도 시작하지 못한 메일"과 "종료 직후 들어온 묶음의 처리"다.

### 해석

- **유실의 한계는 실행기 설정이 아니라 강제 종료까지의 10초다.** 스레드 2개가 10초 동안 보낼 수 있는 양(약 400~600통)을 넘는 작업이 남아 있으면 그만큼 사라진다.
- **종료 대기 8초는 채택하지 않았다.**
  - 무거운 작업량에서 큐의 작업을 버리지 않고 끝까지 돌렸지만, 대부분 10초 뒤에 나가서 실제 배포라면 같은 수가 사라진다.
  - 가벼운 작업량에서는 오히려 나빠졌다. 대기 설정을 켜면 Spring이 종료 초반의 정리 단계를 건너뛰고 실행기를 바로 닫아서, 종료 직후 들어온 묶음이 거절되고 조용히 버려졌다.
- **변경:** 동작은 그대로 두고, 종료할 때 버리는 작업 수를 경고 로그로 남긴다(`AsyncConfig.mailExecutor`의 `shutdown()`). 실험에서 남은 로그는 "대기 중이던 메일 작업 5건을 보내지 않고 버림, 실행 중이던 2건은 중단 신호를 받음"이었다. 이는 실제로 시작하지 못한 메일 410통(100통 묶음 4개 + 10통 묶음 1개)과 일치했다.
- 실제 배포에서는 종료 신호 전에 드레인 30초가 있다. 그동안 이전 컨테이너가 남은 작업을 계속 처리하므로, 실제로 유실되려면 이보다 훨씬 많은 작업이 쌓여 있어야 한다. 이 부분은 측정하지 않았다.

### 버린 시도

- 거절 처리기를 직접 만들어 "종료 중 버림"도 로그로 남기려 했다. 로직은 `CallerRunsPolicy`와 같은데도 "한 번도 시작하지 못한 메일"이 410에서 210으로 바뀌었다. 원인을 설명하지 못해 채택하지 않고 기존 `CallerRunsPolicy`로 되돌렸다. `CallerRunsPolicy`로 되돌린 진단 실행(2회)에서 값이 410으로 돌아오는 것을 확인했다.

### 한계

- 로그는 메일 수가 아니라 **작업(결과 메일 묶음) 수**를 센다. 묶음 하나에 수신자가 몇 명인지, 누구인지는 남지 않는다. 수신자 단위의 기록과 재시작 후 이어서 보내기는 3-B에서 다룬다.
- 종료가 끝난 뒤 들어온 작업을 `CallerRunsPolicy`가 조용히 버리는 경우는 여전히 로그가 없다.
- 실험의 정리 단계 대기 시간은 Spring 기본값(10초)이다. Spring Boot 운영 앱은 기본 30초지만, 어느 쪽이든 배포의 강제 종료(10초)가 먼저 온다.
- 단일 JVM, 로컬 PC에서 쟀다. 실제 `docker compose stop` 과정은 실행하지 않았다.

## U4. FCM 알림 실행기 확인과 분리

### 질문

`FCMService.sendNotification`은 이름 없는 `@Async`였다. 실제로 어느 실행기의 스레드에서 도는가? 메일 실행기를 같이 쓰는가?

### 방법

- `@SpringBootTest`(test 프로필)에서 진짜 `FCMService`(@Async 프록시)를 쓴다. 외부 호출인 `FirebaseMessaging`만 가짜로 바꿔, 호출된 스레드 이름을 기록한다.
- 측정: 알림 30건을 한꺼번에 보낸다. Firebase 호출 한 번에 200ms가 걸린다고 둔다. 생긴 스레드 수, 동시에 Firebase를 부른 수, 30건이 끝난 시간을 기록한다.
- 코드: `src/test/java/org/project/ttokttok/infrastructure/firebase/service/FCMNotificationExecutorTest.java`. 측정은 `measureBurst`이고, 환경 변수가 있을 때만 돈다.

```powershell
$env:TTOKTTOK_FCM_EXPERIMENT_OUT = '<repo>\load-test\results\issue-435\fcm-executor-after.json'
./gradlew.bat cleanTest test --tests "*FCMNotificationExecutorTest"
```

### 결과 (2026-11-11, 로컬 PC)

| 측정 | 변경 전 (`fcm-executor-before.json`) | 변경 후 (`fcm-executor-after.json`) |
|---|---|---|
| 실행 스레드 | `SimpleAsyncTaskExecutor-N` (알림마다 새 스레드) | `fcm-1`, `fcm-2` |
| 알림 30건에 생긴 스레드 | 30개 | 2개 |
| 동시에 Firebase를 부른 수 | 30 | 2 |
| 30건이 모두 끝난 시간 | 0.22초 | 3.2초 |

- 메일 실행기는 같이 쓰지 않았다. 메일은 `mail-` 스레드에서, FCM은 Spring 기본 실행기에서 돌았다. 그래서 "느린 메일 뒤에서 알림이 기다리는" 간섭은 변경 전에도 없었다.
- 원인: 직접 만든 `Executor` 빈(`mailExecutor`)이 있으면 Spring Boot는 기본 실행기(`applicationTaskExecutor`)를 만들지 않는다. 이름 없는 `@Async`는 하나로 정해지는 `TaskExecutor`나 `taskExecutor`라는 이름의 빈을 찾는다. 찾지 못하면 `SimpleAsyncTaskExecutor`를 쓴다.

### 변경

- `AsyncConfig`에 `fcmExecutor`를 추가했다. 스레드 2개, 큐 100, 스레드 이름 `fcm-`이다.
- `FCMService.sendNotification`을 `@Async("fcmExecutor")`로 바꿨다.
- 가득 차면 알림을 버리고 경고를 남긴다. 알림은 `ClubAdminService.toggleRecruitment`의 트랜잭션 안에서 등록된다. 그래서 호출 스레드에서 실행하면(CallerRuns) 발송 동안 DB 커넥션을 쥐고, 예외를 던지면(Abort) 모집 토글이 롤백된다. 모집 시작 알림은 최선 노력으로 보낸다.

회귀 테스트:

- `FCMNotificationExecutorTest.sendsNotificationOnFcmExecutor`: 알림이 `fcm-` 스레드에서 도는지 확인한다. 변경 전에는 실패했다.
- `AsyncConfigTest.fcmExecutorIsBoundedPool`: 실행기 크기를 확인한다.
- `AsyncConfigTest.fcmExecutorDiscardsOverflowWithoutRunningOnCaller`: 넘친 알림이 호출 스레드에서 돌지 않는지 확인한다.

### 해석과 한계

- 알림이 몰려도 스레드와 Firebase 동시 호출이 2개를 넘지 않는다. 대신 한꺼번에 많이 보내면 끝나는 시간이 늘어난다(30건 기준 0.22초에서 3.2초).
- 큐 100칸을 넘는 알림은 버려진다. 지금 규모(동아리 24곳, 모집을 시작할 때만 발송)에서는 일어나기 어렵다고 보지만, 재지 않았다.
- 스레드 2개는 Firebase 쪽 제한을 근거로 정한 값이 아니다. 알림 빈도가 낮아 작게 잡았다.
- 알림 한 건 안의 토큰 묶음 발송(500개씩 순서대로)은 바꾸지 않았다.

## U5. 메일 실행기가 넘칠 때의 정책 결정

### 질문

`mailExecutor`가 가득 찼을 때(실행 중 5, 대기 100) 넘친 결과 메일 작업을 어떻게 처리할 것인가?

### 비교

| 정책 | 넘친 작업 | 관리자 응답 | DB 커넥션 | 근거 |
|---|---|---|---|---|
| **호출 스레드에서 실행 (지금, `CallerRunsPolicy`)** | 요청 스레드가 직접 보낸다. 유실 없음 | 발송이 끝날 때까지 늦어진다(40통, 25ms 기준 약 1.5초) | U1 이후 쥐지 않는다(0.12~0.15초) | U1 |
| 거절 (`AbortPolicy`, 503 같은 응답) | 받지 않는다 | 바로 실패 응답. 관리자가 다시 시도해야 한다 | 쥐지 않는다 | 계산 |
| 버림 (`DiscardPolicy`) | 조용히 사라진다 | "완료" 응답인데 메일이 안 나간다 | 쥐지 않는다 | 계산 |
| 영속 작업 (3-B) | DB에 받아 두고 워커가 보낸다 | 바로 202 응답 | 발송과 무관 | 3-B에서 구현 |

### 결정

3-B 전까지 **`CallerRunsPolicy`를 유지한다.**

- 커넥션 문제는 U1로 해결했다. 남은 비용은 포화 때의 응답 지연이다.
- 거절로 바꾸면 응답은 빨라진다. 하지만 관리자가 다시 시도해야 하고, 다시 눌렀을 때 누가 이미 받았는지 모르는 문제(중복 발송)는 그대로다. API 동작만 바뀌고 근본 문제는 남는다.
- 버림은 "완료"라고 답하고 메일을 보내지 않으므로 받아들일 수 없다.
- 포화가 일어나려면 끝나지 않은 결과 메일 요청이 약 53개 쌓여야 한다(작업 105개 ÷ 요청당 2개). 지금 운영 규모(동아리 24곳)에서는 모든 동아리가 동시에 보내도 작업 48개다.

### 남은 위험

- 포화와 SMTP 무응답이 겹치면, 요청 스레드가 "수신자 수 × 10초(U2 읽기 타임아웃)" 동안 묶인다.
  - 수신자가 12명을 넘으면 Nginx 읽기 타임아웃(120초)을 넘어 관리자는 504를 받는다. 요청 스레드는 그 뒤에도 발송을 계속한다.
  - 이 값은 계산값이고, 재지 않았다.
- 응답 지연과 중복 발송은 3-B에서 다룬다. 작업을 DB에 받아 바로 202로 응답하고, 수신자 단위로 상태를 남기며, 재요청은 멱등 키로 막는다.

## 3-A 정리

| 단위 | 바꾼 것 | 핵심 수치 (변경 전 → 후) |
|---|---|---|
| U1 | 수신자 조회만 읽기 트랜잭션, 커넥션 반납 뒤 발송 등록 | 포화, 동시 10: 요청 1건 커넥션 점유 1.6~1.7초 → 0.12~0.15초, 다른 API 최대 지연 1.5~1.7초 → 0.12~0.15초 |
| U2 | SMTP 연결 5초, 읽기 10초, 쓰기 10초 타임아웃 | 응답 없는 SMTP: 한 통 60초 넘게 막힘 → 10.1초에 실패, 수신자 3명 묶음 60초 넘게 막힘 → 30.1초에 끝남 |
| U3 | 종료 때 버리는 작업 수를 경고 로그로 남김 (종료 대기는 측정 후 채택 안 함) | 1,000통 묶음 종료: 유실 430~443통은 그대로, 로그로 셀 수 있게 됨. 종료 대기 8초는 400통에서 유실 0 → 10통으로 나빠짐 |
| U4 | FCM 전용 실행기(스레드 2개) | 알림 30건: 스레드 30개 → 2개, Firebase 동시 호출 30 → 2, 끝나는 시간 0.22초 → 3.2초 |
| U5 | `CallerRunsPolicy` 유지 결정 | 코드 변경 없음 |

3-A로 해결하지 못한 것은 모두 "작업이 메모리에만 있다"에서 나온다. 재시작 때의 유실, 수신자 단위 결과, 재시도, 중복 발송, 거짓 완료 응답이다. 이것들이 3-B의 범위다.
