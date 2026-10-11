# #426 비교 후보

기준 커밋: `b8b6dc675b1875b845b92a81fd94eb4944e7cc30`.
각 patch는 이 커밋에서 시작한 별도 깨끗한 checkout에 `git apply Vn.patch`로 적용한다.
현재 작업 변경 위에 중첩해서 적용하지 않는다. V0.patch는 변경 없는 기준을 표시한다.

| 후보 | 구현 | 잠금 범위 |
|---|---|---|
| V0 | 기존 조회 후 저장 | 없음, 중복 저장 가능 |
| V1 | UNIQUE + saveAndFlush + 지정 제약 충돌 409 변환 | DB 유일성 검사 |
| V2 | V1 + 사용자 행 PESSIMISTIC_WRITE | 같은 사용자의 서로 다른 모집폼도 직렬화 |
| V3 | V1 + pg_advisory_xact_lock | 사용자·모집폼 조합, hashtext 충돌 시 불필요한 직렬화 가능 |

V1~V3 모두 테스트 DB에 `uk_applicants_user_email_applyform`을 적용해야 한다.
Advisory lock은 PostgreSQL 종속이며 트랜잭션 종료 때 해제된다.
채택하지 않은 후보는 이 patch로만 보존하고 운영 코드에 선택 분기를 넣지 않는다.

## 운영 반영 순서

1. 릴리스 N: 지정 제약 충돌을 409로 처리하는 코드를 먼저 배포한다. 이 단계만으로 중복 저장은 막히지 않는다.
2. 기존 데이터의 중복·NULL을 읽기 전용으로 확인하고 정리 방법을 별도로 승인한다.
3. 릴리스 N+1: 아래 검토용 SQL을 다음 Flyway 버전으로 옮겨 적용한다. 이전 코드가 남아 있는 blue-green 구간까지 충돌 처리가 가능한 상태여야 한다.

```sql
SELECT user_email, applyform_id, COUNT(*)
FROM applicants GROUP BY user_email, applyform_id HAVING COUNT(*) > 1;
SELECT COUNT(*) FROM applicants WHERE user_email IS NULL OR applyform_id IS NULL;
```

두 조회가 0건임을 확인해야 한다. 중복 레코드는 지원서·서류·평가의 연결을 검토하여 처리한다. 자동 삭제하지 않는다.
`next-release-constraint.sql`은 검토용이며 현재 Flyway 경로에 포함되지 않는다.
운영용 마이그레이션은 `src/main/resources/db/migration/V28__add_applicant_user_form_unique_constraint.sql`(#429)이고, 사전 점검 쿼리와 적용 순서는 [deploy/migrations/pending](../../../deploy/migrations/pending/README.md)에 있다. V28의 버전 번호는 실제 릴리스 직전에 다시 확인한다.
ALTER TABLE은 잠금을 유발하므로 데이터 크기와 트래픽에 맞춘 적용 시간을 따로 결정한다.
사전 조회 이후 제약 적용 전에도 새 중복이 생길 수 있다. 제약 생성 시 중복이 발견되면
마이그레이션은 실패·롤백되므로, 반영 직전 쓰기 제어와 재검증 방법을 운영 계획에서 결정한다.
