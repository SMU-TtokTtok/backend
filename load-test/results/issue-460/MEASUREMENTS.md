# #460 결과 메일 발송 작업 DB 이전: 프로세스 강제 종료 측정 기록

| 항목 | 내용 | 결과 파일 |
|---|---|---|
| 본 측정 | 발송 도중 앱을 강제 종료하고 재시작했을 때의 누락·중복 (이전 코드 vs 새 코드, 조건마다 3회) | `crash-before.json`, `crash-after.json` |
| 보조 기록 | 처리량 퇴행을 발견한 측정, 경쟁 조건이 섞인 이전 측정, 첫 검증 회차 | `throughput-before-fix.json`, `crash-after-before-race-fix.json`, `crash-after-trial.json` |
| 스키마 검증 | V32와 엔티티가 실제 PostgreSQL의 `ddl-auto=validate`를 통과하는가 | 아래 "V32 검증" |

## 질문

결과 메일 발송 도중 앱 프로세스가 강제로 죽었다가 같은 jar로 다시 뜨면, 수신자 2,000명 가운데 몇 명이 메일을 못 받고(누락), 몇 명이 두 번 받는가?

#435 측정은 배포 종료(`SIGTERM`) 때 실행기 큐의 작업이 사라지는 것을 쟀다. 이번에는 종료 신호를 받을 틈이 없는 강제 종료다.

## 코드 근거

- **이전 코드 (`d50a4a5`).** `ApplicantAdminService.sendResultMailToApplicants`가 수신자를 읽은 뒤 `EmailService.sendResultMail`을 합격·불합격 두 번 부른다. 이 메서드는 `@Async("mailExecutor")`라서 수신자 목록 전체가 JVM 메모리의 실행기 작업 2개로만 존재한다. 프로세스가 죽으면 남은 수신자 정보는 어디에도 없다.
- **새 코드 (`fix/#460`).** `send-email`은 수신자마다 `result_mail_deliveries` 행(PENDING)을 저장하고 바로 응답한다. 워커(`ResultMailDeliveryScheduler`)가 `FOR UPDATE SKIP LOCKED`로 최대 10행을 잡아 SENDING으로 바꾸고(임대 5분), 트랜잭션 밖에서 한 통씩 보낸 뒤 행마다 SENT를 기록한다. 죽으면 PENDING 행은 그대로 남고, SENDING 행은 임대가 끝난 뒤 다시 잡힌다.
- 보낸 직후 SENT를 기록하기 전에 죽으면 그 행은 한 번 더 나간다. exactly-once가 아니라 at-least-once다.

## 환경

| 항목 | 값 |
|---|---|
| 장비 | 로컬 PC. Windows 11, i7-1280P(14코어 20스레드), RAM 16GB. 운영 서버가 아니다. |
| 런타임 | 실행 JDK 21.0.8(PATH의 `java`), 빌드 대상 Java 17, Spring Boot 3.5.0 |
| 앱 | `java -Xmx512m -jar ...` (jar 두 개, 아래 "jar"). 이 PC의 메모리 압박 때문에 힙을 512MB로 제한했다. 이전·새 코드에 같이 적용했다. |
| DB | Docker `postgres:17.4-alpine` (`ttokttok-426-repro-pg`)의 측정 전용 DB `ttokttok460`. 사용자 `postgres`, 비밀번호 없음. |
| Mailpit | `axllent/mailpit:v1.21.8` (`ttokttok-460-mailpit`). SMTP 127.0.0.1:1025, API 127.0.0.1:8025. **`MP_MAX_MESSAGES=100000`** (기본값은 최근 500통만 보관해 카운트가 틀린다). |
| Redis | `redis:7-alpine` (`ttokttok-460-redis`, 127.0.0.1:6460). JWT 블랙리스트 확인에 필요하다. |
| 메일 발송 | 앱의 `mail.host`는 항상 127.0.0.1:1025. 스크립트가 명령행 인자를 만들 때마다 확인한다. 실제 SMTP로 나가지 않는다. |
| 인증 | 로컬 전용으로 회차마다 새로 만든 JWT 비밀값으로 관리자 토큰을 직접 만든다. 비밀값·토큰은 결과 파일에 남기지 않는다. |
| 새 코드 설정 | `result-mail.worker.poll-delay-ms=5000` (운영 기본값). 임대 5분, 한 번에 10건. |
| 데이터 | 동아리 1곳, 관리자 1명, 활성 폼 1개, 서류 전형 합격 1,000명과 불합격 1,000명. 이메일은 `r<번호>@example.test`. 총 2,000통. `load-test/sql/seed-result-mail.sql` |

### jar

- 새 코드: 브랜치 `fix/#460`의 `19cc700`에서 `./gradlew.bat bootJar`.
- 이전 코드: `git worktree`로 `d50a4a5`를 따로 받아 `bootJar`. 경로는 보고 파일에 있다. 이전 코드는 `result_mail_deliveries` 테이블을 모르며 있어도 무시한다.
- gitignore된 설정 파일은 복사하지 않고 `--spring.config.additional-location=file:<저장소>/src/main/resources/`로 읽는다. 프로파일은 `loadtest`다. DB·메일·Redis·JWT·S3·Firebase 값은 명령행 인자로 덮어쓴다.

## V32 검증 (PostgreSQL 17.4 실제 DB)

Flyway 전체 이력으로 빈 DB를 만들 수 없다(V0가 `ALTER`로 시작). 그래서 다음 순서로 준비했다.

1. 새 코드 jar를 `ddl-auto=create`, `flyway.enabled=false`로 한 번 띄워 전체 스키마를 만들고 종료한다.
2. `result_mail_deliveries`를 DROP하고 `V32__create_result_mail_deliveries_table.sql`을 psql로 적용한다. 오류 없이 `CREATE TABLE`과 인덱스 2개가 만들어졌다(`clubs(id)` 외래 키, CHECK 제약 포함).
3. 측정의 모든 앱 기동(새 코드는 회차마다 2번)을 `ddl-auto=validate`로 했다. Hibernate가 엔티티와 V32로 만든 테이블을 대조하는 검증이며, 전부 `Started TtokttokApplication`까지 통과했다.

결론: V32 SQL은 PostgreSQL 17.4에서 그대로 적용되고, `ResultMailDelivery` 엔티티는 그 테이블과 `validate`를 통과한다. 다만 Flyway가 V32를 적용하는 경로 자체는 이 실험에서 돌리지 않았다(위 이유).

## 조건

- **이전 코드:** `d50a4a5`. **새 코드:** `19cc700`.
- 회차(조건마다 3회)마다 Mailpit을 비우고 seed로 모든 관련 테이블(발송 작업 포함)을 초기화한다.
- 앱을 띄우고 `send-email`(`kind=DOCUMENT`)을 한 번 부른다.
- Mailpit의 수신 수가 500통을 넘는 순간 `taskkill /F /PID <pid>`로 앱을 강제 종료한다. Windows에서 `kill -9`에 해당한다.
  - 새 코드는 종료 직전에 DB에 SENDING 행이 보일 때까지 최대 5초 기다린다. 처리 중인 배치를 죽여 임대 만료 회수 경로를 지나게 하려는 것이다.
- 같은 jar를 같은 인자로 다시 띄운다.
- 수신 수가 30초 동안 늘지 않으면 끝이다. 새 코드는 추가로 PENDING·SENDING이 0이 될 때까지 기다린다. 재시작부터 최대 6분이다.
- 받은 메일 총수, 서로 다른 수신자 수, 누락(2,000 − 서로 다른 수신자), 중복(총수 − 서로 다른 수신자)을 센다.

## 실행

```bash
# 컨테이너 (로컬 전용)
docker exec ttokttok-426-repro-pg createdb -U postgres ttokttok460
docker run -d --name ttokttok-460-mailpit -e MP_MAX_MESSAGES=100000 \
  -p 127.0.0.1:1025:1025 -p 127.0.0.1:8025:8025 axllent/mailpit:v1.21.8
docker run -d --name ttokttok-460-redis -p 127.0.0.1:6460:6379 redis:7-alpine

# 스키마 준비 (새 코드 jar)
python -B -I load-test/scripts/measure-result-mail-crash.py --variant after --jar after.jar --prepare-schema

# 측정
python -B -I load-test/scripts/measure-result-mail-crash.py --variant before --jar before.jar \
  --code-ref d50a4a5 --output load-test/results/issue-460/crash-before.json
python -B -I load-test/scripts/measure-result-mail-crash.py --variant after --jar after.jar \
  --code-ref 19cc700 --output load-test/results/issue-460/crash-after.json
```

스크립트는 회차 시작 전에 여유 메모리가 0.7GB 미만이면 시작하지 않는다.

## 결과: 이전 코드 (`crash-before.json`)

| 회차 | 기대 | 받은 총수 | 서로 다른 수신자 | 누락 | 중복 | 종료 감지 시점 수신 | 종료 뒤 안정된 수신 | 재시작 뒤 추가 수신 |
|---|---|---|---|---|---|---|---|---|
| 1 | 2,000 | 570 | 570 | **1,430** | 0 | 506 | 570 | 0 |
| 2 | 2,000 | 549 | 549 | **1,451** | 0 | 501 | 549 | 0 |
| 3 | 2,000 | 562 | 562 | **1,438** | 0 | 503 | 562 | 0 |

## 결과: 새 코드 (`crash-after.json`, poll 5000ms)

| 회차 | 기대 | 받은 총수 | 서로 다른 수신자 | 누락 | 중복 | 종료 시점 DB 상태 (SENT/SENDING/PENDING) | 회수된 행 | 재시작 → PENDING 0 | 재시작 → 마지막 메일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 2,000 | 2,000 | 2,000 | **0** | 0 | 527 / 3 / 1,470 | 3 | 39.9초 | 300.2초 |
| 2 | 2,000 | 2,000 | 2,000 | **0** | 0 | 530 / 0 / 1,470 | 0 | 36.4초 | 35.9초 |
| 3 | 2,000 | 2,001 | 2,000 | **0** | **1** | 527 / 3 / 1,470 | 3 | 41.6초 | 301.9초 |

- 모든 회차의 최종 상태는 `SENT 2,000`이다. PENDING·SENDING·FAILED가 남지 않았다.
- 회수된 행은 전부 `attempt_count = 2`다. 임대 만료까지 299.996초, 최종 발송은 청구 후 302.3~304.0초(임대 만료 후 2.3~4.0초)였다.
- 3회차의 중복 1통: 종료 시점 DB의 SENT는 527행인데 Mailpit이 받은 메일은 528통이었다. 한 통은 SMTP가 받았지만 SENT를 기록하기 전에 죽었고, 그 행이 회수돼 한 번 더 나갔다.
- 2회차는 임대 회수 경로를 지나지 않았다. 종료 직전 확인에서는 SENDING 행이 보였지만, 그 뒤 실제로 죽는 순간에는 배치가 막 끝나 처리 중인 행이 없었다(종료 시점 상태 SENDING 0). 배치 사이에 죽은 경우로, PENDING 행만 재시작 뒤 이어 보냈다. 그래서 이 회차는 PENDING 이어 보내기의 근거이고, 임대 만료 회수의 근거는 1·3회차다.

### 시간을 둘로 나눠 읽는다

"재시작 → 마지막 메일"이 회차마다 300초 또는 36초로 갈리는 이유는 다음 두 시간이 겹쳐 있기 때문이다.

| 구간 | 의미 | 측정 |
|---|---|---|
| 남은 PENDING 발송 | 죽을 때 아직 잡히지 않은 1,470통을 재시작한 앱이 보내는 시간 | 재시작 뒤 앱이 뜨고(약 11~12초) 25.5~29.9초 동안에 1,470통을 보냈다. 약 49~58통/초 |
| 처리 중이던 행의 회수 지연 | SENDING이던 행은 임대(5분)가 끝나야 다시 잡힌다 | 청구 후 302.3~304.0초. 죽은 시점이 아니라 **청구 시점부터 5분**이다 |

처리 중이던 행이 없는 2회차만 36초에 끝났다.

## 결과 해석

1. **이전 코드:** 강제 종료 시점에 이미 받은 수신자 말고는 모두 사라졌다. 3회 모두 재시작 뒤 새 메일이 한 통도 오지 않았다. 누락은 1,430~1,451명(전체의 약 72%)이다. 종료 감지 시점(500통대)보다 받은 수가 많은 것(549~570)은 Mailpit API 집계가 SMTP 수신보다 늦기 때문이다. 앱이 이미 SMTP로 넘긴 메일이 Mailpit 안에서 반영되는 중에 죽은 것이다.
2. **새 코드:** 3회 모두 누락 0이다. PENDING 행은 재시작한 앱이 이어서 보내고, 처리 중이던 SENDING 행은 임대가 끝난 뒤 회수해 보냈다.
3. **중복은 많아야 1통이다.** 워커가 한 통씩 순차로 보내므로 한 번 죽을 때 "SMTP는 받았지만 SENT를 기록하지 못한" 행은 보내던 그 한 통뿐이다. 측정한 중복은 0, 0, 1통이다. 같은 코드·설정으로 돌린 첫 검증 회차(`crash-after-trial.json`)도 SENDING 4행 중 1통이 중복이었다. 이 상한은 설계에서 나온 값이고, 측정은 그 범위에 맞는다.
4. 임대 5분 때문에, 처리 중에 죽은 수신자는 최대 약 5분 늦게 받는다. 이 비용은 누락 0·중복 ≤1의 대가다. 임대를 줄이면 느린 SMTP 응답 중인 정상 발송을 다른 워커가 가로챌 위험이 커진다.

## 측정으로 발견해 고친 것: 처리량 퇴행

처음 새 코드는 워커가 한 번 깨어날 때마다 10건만 보내고 다음 폴링(`poll-delay-ms`)까지 쉬었다. 첫 측정(`throughput-before-fix.json`, 수정 전 `d989c8f`, poll 1,000ms)에서 재시작 뒤 1,490통을 보내는 데 196.7초(앱 기동 약 15초 포함)가 걸렸다. 약 7.6통/초(기동 제외 약 8.2통/초)다. 운영 기본값 5초면 약 2통/초로 2,000명에 약 17분이 걸린다. 이전 코드는 스레드 2개가 쉬지 않고 보냈으므로 받아들일 수 없는 퇴행이었다.

- 수정: `ResultMailDeliveryScheduler.sendDueMails()`가 가져온 건수가 `CLAIM_SIZE`(10)와 같으면 쉬지 않고 다음 배치를 가져온다. 10건보다 적으면 끝낸다. 회귀 테스트는 `ResultMailDeliverySchedulerTest`(claimDue가 10건, 3건을 돌려줄 때 한 번 호출에 13건이 모두 나가고 claimDue는 2번 불린다). 먼저 실패하는 것을 확인했다. 커밋 `19cc700`.
- 수정 뒤(poll 5,000ms): 같은 1,470통을 25.5~29.9초(약 49~58통/초)에 보냈다. 수정 전 약 8.2통/초(기동 제외)의 약 6~7배다. 폴링 간격은 이제 처리량에 거의 영향이 없고, 배치가 끝난 뒤 첫 폴링을 기다리는 시간만 더한다.

## 측정 도구의 경쟁 조건과 이전 측정의 2회차 이상치

수정 직후 처음 돌린 `after` 3회(`crash-after-before-race-fix.json`)의 2회차는 "종료 시점에 SENDING 1건이 있었고 회수됐는데(`rows_with_attempt_count_over_1=1`) 재시작부터 마지막 메일까지 38.1초"로 나왔다. 임대 만료보다 빨리 같은 행을 다시 가져간(= 다른 워커가 가로챈) 것처럼 보여 조사했다.

- 앱 로그가 답이다. 그 회차 두 번째 앱의 마지막 배치(1건)는 05:08:42.250에 나갔다. 첫 앱의 마지막 배치가 05:03:41.4이고 SENDING 행의 임대가 5분이므로 정확히 임대 만료 직후다. 처리 중인 행을 일찍 가로챈 것이 아니다.
- 원인은 측정 도구였다. 대기 루프가 Mailpit 수신 수를 먼저 읽고 DB 상태를 나중에 읽었다. 마지막 메일이 나가 SENT로 기록되는 순간에는 Mailpit API 집계가 아직 반영되기 전이라, 도구는 "수신 수 1,999, DB는 모두 SENT"를 보고 끝냈다. 마지막 메일은 그 뒤 집계에 잡혔지만 시각 기록(`seconds_restart_to_last_mail`)에는 들어가지 않았다. 최종 개수(2,000)는 맞았고, 틀린 것은 마지막 메일 시각 하나였다.
- 고침: DB를 먼저 읽고 Mailpit을 나중에 읽는다. 끝난 뒤 수신 수가 안정되는 동안 늘어난 메일이 있으면 그 시각을 마지막 메일 시각으로 잡는다. 종료 시점 SENDING 행마다 청구·임대 만료·최종 발송 시각을 `recovered_rows`로 남긴다.
- 그 측정의 누락·중복(0·0, 0·0, 0·0)과 최종 상태는 맞다. 시각만 이 문서의 표(`crash-after.json`, 수정 뒤 재측정)로 대체했다. 이전 파일은 비교용으로 남겼다.

## 한계

- **로컬 PC, 단일 앱 인스턴스, 로컬 PostgreSQL이다.** 여러 인스턴스가 동시에 워커를 돌릴 때(`SKIP LOCKED` 경쟁)의 강제 종료는 이 측정이 다루지 않는다. 그 동작은 PostgreSQL 통합 테스트(`ResultMailDeliveryPostgresTest`)에서 따로 본다.
- **Mailpit은 실제 Postfix가 아니다.** SMTP 응답 지연이 없고, API 집계가 SMTP 수신보다 몇 십 통 늦는다(종료 감지 500통대 → 안정 후 527~570통). 운영 Postfix·네이버 릴레이의 지연과 거절은 재현하지 않는다.
- **exactly-once가 아니다.** 보낸 뒤 SENT를 기록하기 전에 죽으면 한 통이 한 번 더 나간다. 같은 코드로 돌린 7회(본 측정 3회, 시각 오류가 있던 3회, 첫 검증 1회) 중 2회에서 1통씩 나왔다. 횟수가 적어 중복이 나오는 빈도는 말할 수 없다.
- **종료 시점은 수신 500통대 한 곳뿐이다.** 다른 시점(발송 직후, 기록 직전)을 훑지 않았다. 새 코드의 SENDING 행 수는 종료 때마다 0~6건으로 달랐다.
- 조건마다 3회다. 시간 값은 중앙값보다 범위로 읽어야 한다.
- 회수 행의 5분 지연은 임대 설정값(5분)이 그대로 드러난 것이다. 운영에서 중간 종료가 일어나면 그 수신자는 최대 약 5분 늦게 받는다.
- 실행 JDK는 21.0.8이고 빌드 대상은 Java 17이다. 힙은 `-Xmx512m`로 제한했다. 운영 JVM 설정과 다르다.
- 이전 코드 측정은 한 번의 `send-email`이 두 개의 `mailExecutor` 작업(합격·불합격)을 만드는 구조를 그대로 썼다. 종료 시점 이후의 수신 증가분은 Mailpit의 집계 지연 때문이며, 재시작 뒤 앱이 보낸 것이 아니다(재시작 뒤 추가 수신 0).
