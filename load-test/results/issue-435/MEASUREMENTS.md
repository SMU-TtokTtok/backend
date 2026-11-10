# #435 결과 메일 API의 커넥션 점유 (변경 전 기준선)

## 질문

`mailExecutor`가 가득 찼을 때 결과 메일 API(`POST /api/admin/applies/{clubId}/send-email`)는 DB 커넥션을 발송이 끝날 때까지 쥐고 있는가? 그동안 다른 API는 어떤 영향을 받는가?

## 코드 근거

- `ApplicantAdminService.sendResultMailToApplicants`는 `@Transactional(readOnly = true)`이다. 이 트랜잭션 안에서 `emailService.sendResultMail`을 합격, 불합격 두 번 호출한다.
- `sendResultMail`은 `@Async("mailExecutor")`다. 실행기는 core 2, max 5, 큐 100, `CallerRunsPolicy`다.
- 실행기가 가득 차면 거절된 작업이 호출 스레드에서 동기로 실행된다. 호출 스레드는 트랜잭션을 연 요청 스레드다.

## 한 통에 걸리는 시간: 운영 실측 (2026-11-10, 읽기 전용)

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

## 환경

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

## 조건

- **NORMAL:** 실행기가 비어 있다.
- **SATURATED:** 끝나지 않는 작업으로 실행기를 채운다(실행 중 5, 큐 100). 그 상태에서 API를 부르면 작업 2개가 모두 거절되어 요청 스레드에서 실행된다.
- 동시 요청 수는 1, 5, 10이다. 서로 다른 동아리가 그 수만큼 같은 순간에 요청한다.

## 실행

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

## 결과 (`before.json`, 2026-11-10 19:50~19:52)

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

## 해석

- 실행기가 포화되면 결과 메일 요청 하나가 커넥션 1개를 **발송 전체 시간** 동안 쥔다. 평소에는 0.3초 이내에 반납한다.
- 40통에 약 1.5초가 걸렸다. 40 × 25ms = 1.0초에 메일 생성 같은 통당 오버헤드가 더해졌다(통당 약 12ms로 추정한다).
- 점유 시간은 "수신자 수 × 한 통 시간"에 비례한다. 요청이 커넥션 수만큼 겹치면 메일과 무관한 API가 그 시간만큼 기다린다. 점유가 획득 대기 한도(30초)를 넘으면 그 API는 실패한다.
- 실측 조건에서 30초를 넘으려면 요청당 수신자가 약 800명 이상이어야 한다(30초 ÷ 통당 약 37ms).
- 다만 한 통 시간에는 상한이 없다. SMTP 타임아웃 설정이 없어서, Postfix가 느려지거나 응답하지 않으면 그만큼 길어진다.
- 응답도 발송이 끝날 때까지 나가지 않는다. 이 문제는 커넥션 분리로 해결되지 않으므로 따로 다룬다.

## 한계

- 포화는 끝나지 않는 작업 105개로 **강제로 만든 상태**다. 운영에서 실제로 일어난 기록은 없다. 결과 발표 한 번이 작업 2개이므로, 끝나지 않은 요청이 약 53개 쌓여야 이 상태가 된다.
- 운영 DB 기준 지원자가 있는 폼은 3개, 지원자는 7명이다. 평가를 종료하면 지원자가 지워지므로 과거 규모는 알 수 없다. 지금 규모에서는 포화가 사실상 일어나지 않는다.
- 한 통 시간의 실측값은 운영 Postfix 로그 9건에서 나왔으므로 표본이 작다. 앱 쪽의 TCP 연결과 `EHLO` 시간은 이 값에 들어 있지 않다.
- 단일 JVM, 로컬 PC, 로컬 PostgreSQL에서 측정했다.
- NORMAL 동시 10에서 공지 API p95가 155~254ms로 오른 것은, 요청 10개가 동시에 시작하며 커넥션을 잠깐 모두 쓴 순간 때문이다(샘플 38~39개 중 3~4개에서 풀이 가득 참). 메일 발송과는 관계없다.
