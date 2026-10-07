# #426 동시 지원 중복 저장 재현

작성일: 2026-10-06. 아래 관찰은 수정 전 기준 코드의 재현 기록이다.
해결 후보의 실제 HTTP 비교 조건은 [BENCHMARK.md](BENCHMARK.md), 최종 비교는 MEASUREMENTS.md에 기록한다.

## 관찰 결과

| 시나리오 | HTTP 응답 | 지원서 | 서류 전형 |
|---|---|---:|---:|
| 동일 사용자·모집폼 순차 2회 | 200, 409 | 1 | 1 |
| 중복 검사 직후 겹치게 만든 2회 | 200, 200 | 2 | 2 |
| 시작 시점만 맞춘 동시 20회 | 200 × 20 | 20 | 20 |

[baseline.json](baseline.json)은 성공한 재현 실행의 응답·건수·제약 및 테스트 판정을 추출한 원본 요약이다. 요청 종료 후 JDBC로 커밋된 지원서와 연결된 서류 전형 건수를 직접 확인했다.

## 재현 조건

- 코드 기준: b8b6dc675b1875b845b92a81fd94eb4944e7cc30, fix/#426.
- 독립 Docker PostgreSQL 17.4-alpine, DB ttokttok426, 127.0.0.1의 임의 포트.
- Hibernate create-drop으로 현재 엔티티의 스키마 생성, Flyway 비활성. 운영 DB의 실제 제약이나 데이터는 확인하지 않았다.
- 실제 JWT·보안 필터·MockMvc·서비스·JPA·JDBC 사용. 테스트 자체에는 트랜잭션이 없어 요청별 서비스 트랜잭션으로 처리·커밋한다.
- 파일 없는 유효한 텍스트 답변, 동일 로그인 사용자·모집폼. 임시 지원서 fixture는 없다. S3/SMTP/FCM은 mock이며 미호출도 검증한다.
- 순차/동시 시나리오는 각각 고유한 사용자·동아리·모집폼을 사용한다.
- 2회 재현은 AnswerAssembler spy에 barrier를 넣어 실제 중복 조회 직후 두 요청이 대기하게 한다. Repository 결과를 조작하지 않으며 이후 실제 답변 조립을 호출한다.
- 20회 재현은 이 barrier 없이 시작 시점만 맞춘다. 요청 스레드 최대 20, 커넥션 풀 상한 30, 대기 시간 제한 적용.
- HTTP 실행은 MockMvc이므로 실제 네트워크 부하 및 처리 성능 측정은 아니다.
- 처음에는 테스트용 동아리 이름이 겹쳐 fixture 생성에 실패했다. 고유한 이름으로 수정한 뒤 재실행한 결과를 기록했다.

## 실제 중복 성공 시나리오

```text
A: existsByUserEmailAndApplyFormId → false ─┐
B: existsByUserEmailAndApplyFormId → false ─┤ 둘 다 중복 검사 통과
                                         │
A: 실제 답변 조립 → UUID A 지원서·서류 저장 → COMMIT → HTTP 200
B: 실제 답변 조립 → UUID B 지원서·서류 저장 → COMMIT → HTTP 200

같은 user_email + applyform_id: 지원서 2건, 서류 전형 2건
```

각 지원서의 UUID가 달라 PK 충돌이 없다. 테스트 applicants 테이블에는 PK·FK·CHECK만 있고 사용자·모집폼 조합의 UNIQUE는 없었다. 서류 전형의 applicant_id 유일성도 서로 다른 지원서 두 개를 막지 못한다.

## 해결책 비교

| 대안 | 장점 | 비용·한계 |
|---|---|---|
| **복합 UNIQUE + 해당 충돌만 409로 변환 (우선 추천)** | DB가 같은 사용자·모집폼 조합의 중복 저장을 막음; 여러 서버·다른 저장 경로에도 적용 | 중복/NULL 데이터 확인, 단계적 배포, 실제 flush/commit 충돌 처리 필요. 파일 업로드는 별도 문제 |
| 사용자 행 비관적 잠금 후 중복 재조회 | 기존 사용자 행을 이용해 조회·저장을 직렬화; 스키마 변경 불필요 | 동일 사용자의 다른 모집폼 지원도 대기. 모든 저장 경로가 같은 잠금 규약을 따라야 함 |
| PostgreSQL 트랜잭션 advisory lock | 사용자·모집폼 조합 단위 잠금; 스키마 변경 불필요 | PostgreSQL 의존·키 규약 필요. 참여하지 않는 저장 경로는 보호 못 함; 해시 충돌 시 불필요한 대기 가능 |

빈 Applicant 조회에 비관적 잠금을 거는 것으로는 아직 없는 지원서의 삽입을 보호할 수 없다. synchronized는 여러 서버를 보호하지 못하고, Redis 분산 잠금은 현재 DB 기반 해결책보다 구성·장애 조건이 늘어나므로 먼저 도입하지 않는다.

공식 근거: [복합 UNIQUE](https://www.postgresql.org/docs/17/ddl-constraints.html#DDL-CONSTRAINTS-UNIQUE-CONSTRAINTS), [행 잠금과 advisory lock](https://www.postgresql.org/docs/17/explicit-locking.html).

## 다음 실험

1. 테스트 DB에 UNIQUE를 적용해 중복 저장 차단을 확인한다. 500이 발생하면 해결 완료가 아니다.
2. 해당 제약 충돌만 기존 AlreadyApplicantExistsException(409)으로 처리하고 저장·롤백 시점을 확인한다.
3. 같은 2회/20회 실험을 다시 실행해 성공 1회, 나머지 409, 지원서·서류 전형 각각 1건을 검증한다. 다른 사용자·모집폼 및 임시 지원폼 회귀도 추가한다.
4. 운영 반영은 기존 데이터·NULL 확인 후 단계적 배포한다. 저장소 규칙상 먼저 충돌 처리가 가능한 코드를 배포하고 후속 릴리스에서 제약을 추가한다.

위 내용은 최초 재현 시점의 상태이다. 이후 서비스에 지정 UNIQUE 충돌을 409로 변환하는 처리를 추가했다.
현재 Flyway에는 UNIQUE를 추가하지 않았다. [후속 릴리스 SQL과 순서](../../variants/issue-b/README.md)를 따른다.
파일 지원의 잔여 S3 파일, 모집 정원 동시성, 운영 DB 정리·배포는 이번 범위 밖이다.

## 재실행

처음에는 아래 전용 컨테이너를 생성한다. 이미 준비된 컨테이너는 docker start ttokttok-426-repro-pg로 시작한다.

```powershell
docker run -d --name ttokttok-426-repro-pg -p '127.0.0.1::5432' -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_DB=ttokttok426 postgres:17.4-alpine
$taskPort = (docker port ttokttok-426-repro-pg 5432/tcp).Trim().Split(':')[-1]
$env:TTOKTTOK_CONCURRENCY_JDBC_URL = "jdbc:postgresql://127.0.0.1:$taskPort/ttokttok426"
.\gradlew.bat test --tests '*ApplicantDuplicateApplyPostgresTest'
```

환경변수는 전용 PowerShell 세션에서 설정하고 세션 종료로 해제한다. 테스트 종료 시 create-drop으로 테스트 스키마를 제거한다. 컨테이너는 docker stop ttokttok-426-repro-pg로 정지할 수 있다.

최초 기준 코드에서 동시성 테스트 2개가 실패했다. 현재 회귀 테스트는 독립 스키마에 UNIQUE를 추가하고
순차·겹친 두 요청·20요청·다른 사용자·임시 지원서 롤백의 5개 사례를 검증한다.
환경변수 없는 기본 빌드에서는 이 5개 테스트가 제외된다. 기본 빌드 성공만으로 해결을 주장하지 않는다.
