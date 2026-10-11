# #432 합격 확정 중복 부원 등록 방지 후보 비교

작성일: 2026-10-11. 부원 동일성은 (동아리, 이메일)이다.
원본은 [experiment.json](experiment.json), 실행 코드는 `ApplicantFinalizeRaceExperimentTest`.

**선택: V4 = 동아리 행 잠금 + (후속 릴리스에서) UNIQUE(club_id, email) 백스톱.**
첫 릴리스는 잠금만 배포하고, 제약은 #426/#429와 같은 순서(사전 조회 → 후속 릴리스)로 뒤따른다.

## 후보

| 후보 | 방식 |
|---|---|
| V0 | 현재 코드: 지원자별 `existsByClubIdAndEmail` 후 `saveAll` |
| V1 / V1S | UNIQUE + V0 흐름. V1은 행 처리 순서가 요청마다 다름, V1S는 이메일 정렬 |
| V2 / V2S | UNIQUE + `INSERT ... ON CONFLICT DO NOTHING`. V2는 순서 임의, V2S는 이메일 정렬 |
| V3 | `clubs` 행 `PESSIMISTIC_WRITE` 잠금(Hibernate/PostgreSQL) 후 V0 흐름. 제약 없음 |
| V4 | V3 + UNIQUE |
| V5 | `pg_advisory_xact_lock(hashtext(club_id))` 후 V0 흐름. 제약 없음 |

## 실험 조건

- PostgreSQL 17.4(alpine, Docker 전용 컨테이너 `ttokttok-426-repro-pg`, 2 CPU/1GB), 전용 스키마 `finalize_race_experiment`, 격리 수준 READ COMMITTED(기본).
- 테스트 JVM 안에서 스레드 20개가 CountDownLatch로 동시에 시작해 서비스 계층 로직을 각자 별도 트랜잭션·커넥션(Hikari 60)으로 실행한다. HTTP·JWT·필터는 거치지 않는다.
- 동아리당 합격자 30명(서류 전형 PASS, 면접 없음 → 바로 부원 등록). 후보당 20회 반복(mixed는 40회). 반복마다 새 동아리.
- SQL 로깅은 끈다. 운영 DB·실제 수신자는 사용하지 않는다.
- 재실행 명령은 아래와 같다.

```powershell
docker start ttokttok-426-repro-pg
$port = (docker port ttokttok-426-repro-pg 5432).Split(':')[-1]
$env:TTOKTTOK_CONCURRENCY_JDBC_URL = "jdbc:postgresql://127.0.0.1:$port/ttokttok426"
$env:TTOKTTOK_EXPERIMENT_OUT = 'load-test/results/issue-432/experiment.json'
.\gradlew.bat cleanTest test --tests '*ApplicantFinalizeRaceExperimentTest' -x jacocoTestReport
```

`cleanTest`가 없으면 Gradle이 test 태스크를 up-to-date로 건너뛴다(환경 변수는 입력으로 취급되지 않는다).

## 1. 같은 동아리에 합격 확정 20개 동시 요청

| 후보 | 응답 분포 (요청 수) | 중복 발생 반복 | 중복 그룹 | 성공 지연 p50 / p95 ms | 반복 전체 소요 p50 / p95 ms |
|---|---|---:|---:|---:|---:|
| V0_CHECK_SAVE | OK 400 | 20/20 | 600 | 511.9 / 657.5 | 520.8 / 657.6 |
| V1_UNIQUE_SHUFFLED | DEADLOCK 376, OK 20, UNIQUE_CONFLICT 4 | 0/20 | 0 | 16263.6 / 18358.4 | 16264.3 / 18358.6 |
| V1S_UNIQUE_SORTED | OK 20, UNIQUE_CONFLICT 380 | 0/20 | 0 | 217.4 / 225.9 | 219.8 / 228.3 |
| V2_UPSERT_SHUFFLED | DEADLOCK 374, OK 26 | 0/20 | 0 | 16208.3 / 18333.6 | 16244.8 / 19167.2 |
| V2S_UPSERT_SORTED | OK 400 | 0/20 | 0 | 224.4 / 282.7 | 227.1 / 282.9 |
| V3_ROW_LOCK | OK 400 | 0/20 | 0 | 1066.8 / 1965.6 | 1937.1 / 2240.1 |
| V4_ROW_LOCK_UNIQUE | OK 400 | 0/20 | 0 | 970.2 / 1820.6 | 1833.1 / 2079.2 |
| V5_ADVISORY_LOCK | OK 400 | 0/20 | 0 | 965.4 / 1786.9 | 1787.0 / 2006.4 |

- **V0는 오류 없이 중복을 만든다.** 400개 요청이 모두 성공으로 응답했고, 20회 반복 모두 합격자 30명 전원이 중복 등록됐다(그룹 600). 실제 서비스(`ApplicantAdminService.finalizeApplicantsStatus`)로 같은 부하를 실행해도 동일했다(중복 20/20, 그룹 600, 응답은 매번 `passedCount=30, totalFinalizedCount=30`).
- **UNIQUE에서 행 처리 순서가 요청마다 다르면 교착 상태가 난다**(V1 376/400, V2 374/400, PostgreSQL deadlock_timeout 1초 탓에 반복당 약 16초). 이메일 정렬(V1S, V2S)로 사라진다.
- **V1S는 1개만 성공하고 나머지는 UNIQUE 충돌 오류**가 난다. 반복 요청이 같은 성공 결과를 받는 것이 아니라 오류를 받는다.
- **V2S는 400개 모두 성공, 중복 0, p95 283ms로 가장 빠르다.**
- **잠금 후보(V3~V5)는 모두 성공, 중복 0.** 같은 동아리 요청이 직렬화되어 반복 전체 소요가 약 1.8~1.9초(요청 1개당 약 90~100ms씩 대기)로 늘어난다.

## 2. 서로 다른 동아리 20개 동시 요청 (직렬화 부작용 확인)

| 후보 | 응답 분포 (요청 수) | 중복 발생 반복 | 중복 그룹 | 성공 지연 p50 / p95 ms | 반복 전체 소요 p50 / p95 ms |
|---|---|---:|---:|---:|---:|
| V0_CHECK_SAVE | OK 400 | 0/20 | 0 | 423.7 / 637.6 | 424.1 / 638.1 |
| V1_UNIQUE_SHUFFLED | OK 400 | 0/20 | 0 | 294.3 / 341.6 | 298.4 / 343.0 |
| V1S_UNIQUE_SORTED | OK 400 | 0/20 | 0 | 309.3 / 340.7 | 316.4 / 341.0 |
| V2_UPSERT_SHUFFLED | OK 400 | 0/20 | 0 | 217.6 / 244.9 | 222.6 / 245.1 |
| V2S_UPSERT_SORTED | OK 400 | 0/20 | 0 | 220.0 / 290.4 | 228.4 / 290.9 |
| V3_ROW_LOCK | OK 400 | 0/20 | 0 | 527.4 / 713.5 | 527.8 / 713.6 |
| V4_ROW_LOCK_UNIQUE | OK 400 | 0/20 | 0 | 348.9 / 399.6 | 365.0 / 404.4 |
| V5_ADVISORY_LOCK | OK 400 | 0/20 | 0 | 620.7 / 730.2 | 622.2 / 730.3 |

모든 후보가 오류·중복 없이 성공했다. 잠금 후보(V3~V5)의 지연은 V0 대비 큰 차이가 없고 후보 간 차이는 반복 편차 수준이다. 잠금이 다른 동아리의 요청을 직렬화하지 않는다.

## 3. 합격 확정과 수동 부원 등록이 같은 (동아리, 이메일) 한 건을 두고 경쟁

동시 요청 20개 중 10개는 합격 확정, 10개는 수동 부원 등록(`ClubMemberService.createClubMember`와 같은 조회 후 저장). `manual_takes_lock`은 수동 등록도 같은 잠금을 잡는 경우, `manual_no_lock`은 잠금을 잡지 않는 경우다.

| 후보 | 응답 분포 (요청 수) | 중복 발생 반복 | 중복 그룹 | 성공 지연 p50 / p95 ms | 반복 전체 소요 p50 / p95 ms |
|---|---|---:|---:|---:|---:|
| V0_CHECK_SAVE | OK 800 | 40/40 | 40 | 16.6 / 80.1 | 57.6 / 87.4 |
| V2S_UPSERT_SORTED | OK 440, UNIQUE_CONFLICT 360 | 0/40 | 0 | 17.9 / 65.5 | 55.5 / 86.1 |
| V3_ROW_LOCK+manual_takes_lock | ALREADY_MEMBER_409 382, OK 418 | 0/40 | 0 | 82.5 / 157.3 | 157.6 / 177.0 |
| V3_ROW_LOCK+manual_no_lock | OK 800 | 40/40 | 40 | 10.3 / 136.2 | 140.8 / 155.6 |
| V4_ROW_LOCK_UNIQUE+manual_takes_lock | ALREADY_MEMBER_409 380, OK 420 | 0/40 | 0 | 85.8 / 162.8 | 163.5 / 177.3 |
| V4_ROW_LOCK_UNIQUE+manual_no_lock | OK 440, UNIQUE_CONFLICT 360 | 0/40 | 0 | 73.3 / 145.5 | 146.3 / 159.4 |
| V5_ADVISORY_LOCK+manual_takes_lock | ALREADY_MEMBER_409 382, OK 418 | 0/40 | 0 | 92.1 / 167.2 | 169.7 / 183.9 |
| V5_ADVISORY_LOCK+manual_no_lock | OK 800 | 40/40 | 40 | 14.4 / 132.6 | 138.5 / 152.7 |

- 잠금만 쓰는 후보(V3, V5)는 **잠금을 잡지 않는 쓰기 경로가 하나라도 있으면 40/40 반복에서 다시 중복**이 생긴다. 잠금은 협조하는 쓰기 경로 사이에서만 보장된다.
- UNIQUE가 있으면(V2S, V4) 잠금을 안 잡는 경로가 있어도 중복 0이고, 진 쪽이 UNIQUE 충돌 오류를 받는다. 조용한 중복 대신 큰 소리로 실패한다.
- 잠금을 모두 잡는 경우(`manual_takes_lock`) 수동 등록의 패자는 기존 규약대로 `AlreadyClubMemberException`(409)을 받고 합격 확정은 모두 성공한다.

## 4. 잠금 범위

동아리 행을 잠근 채 같은 동아리를 참조하는 부원 INSERT를 실행하면 대기 없이 6.7ms에 끝났다(`blocked=false`). 동아리 행 잠금이 FK 확인을 막지 않는다. 발행된 SQL(`FOR NO KEY UPDATE` 여부)은 로그로 직접 확인하지 않았고 이 결과로 추정한다.

## 트레이드오프와 선택 이유

| 기준 | V2S (UNIQUE + ON CONFLICT) | V3 (행 잠금만) | V4 (행 잠금 + UNIQUE) |
|---|---|---|---|
| 잠금 없는 쓰기 경로가 있어도 중복 0 | O | X (40/40 중복) | O |
| 반복·동시 확정이 같은 성공 응답 | O | O | O (협조 경로 간) |
| 코드 선배포 가능 (제약 없이 동작) | X (`ON CONFLICT (club_id,email)`는 제약이 있어야 동작) | O | O (제약은 후속 릴리스) |
| 이식성 | PostgreSQL 전용 네이티브 SQL, 엔티티 감사 필드 수동 처리 | JPA 표준 락 | JPA 표준 락 + 제약 |
| 같은 동아리 동시 확정 비용 | 가장 낮음(p95 283ms) | 요청당 약 90~100ms 직렬 대기 | 동일 |
| 행 처리 순서 | 정렬 필수 (아니면 교착) | 무관 | 무관 |

V2S가 가장 빠르지만 이 저장소의 확장-수축 규칙(코드 선배포 → 사전 조회 → 제약 릴리스)과 맞지 않고 네이티브 SQL이 필요하다. 합격 확정은 동아리당 전형마다 드문 관리자 작업이라 같은 동아리 동시 요청의 직렬 대기 비용(약 100ms/요청)은 수용할 수 있다고 판단한다. 따라서 V4를 택해 **먼저 잠금으로 멱등성을 확보하고, 이후 UNIQUE로 어떤 쓰기 경로에서도 중복을 막는다.**

## 한계

- 같은 머신의 단일 테스트 JVM에서 스레드로 실행했다. HTTP·보안 필터·두 번째 앱 인스턴스는 거치지 않았다(잠금과 제약은 DB 수준이라 같은 원리이지만 이 실험에서 측정하지는 않았다).
- 합격자 30명, 동시 20개. 다른 규모에서의 분포는 측정하지 않았다. 지연은 개발 머신 편차가 크므로 후보 간 작은 차이(특히 2절)는 해석하지 않는다.
- 운영 `club_members` 스키마와 기존 중복 여부는 확인하지 못했다. UNIQUE 적용 전 사전 조회가 필요하다.
- 락 대기 시간 상한(타임아웃)은 설정하지 않았다. 대기가 길어지는 경우의 동작은 측정하지 않았다.
- 실험 하네스의 후보 로직은 서비스 코드와 별개로 이 테스트 안에서 구현했다. V0만 실제 서비스로 교차 확인했다.
