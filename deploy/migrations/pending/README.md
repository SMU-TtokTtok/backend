# #426 UNIQUE 제약 적용 사전 점검

이 폴더는 기본 Flyway 경로(`classpath:db/migration`) 밖이다. 여기의 SQL은 자동 실행되지 않는다.
제약 SQL은 #429에서 `src/main/resources/db/migration/V28__add_applicant_user_form_unique_constraint.sql`로 옮겼다.
이 폴더에는 적용 전 읽기 전용 점검 SQL(`check_applicant_duplicates.sql`)과 적용 절차가 남아 있다.

## #426 적용 순서

1. `uk_applicants_user_email_applyform` 충돌을 HTTP409로 처리하는 코드를 먼저 배포하고 이전 코드의 종료를 확인한다.
2. `check_applicant_duplicates.sql`로 중복·NULL·기존 제약/인덱스를 읽기 전용 점검한다. 기존 데이터가 발견되면 연결된 서류·평가를 고려해 정리 방안을 별도로 승인받는다.
3. 테이블 잠금과 작업 시간을 검토하고, 사전 조회 이후 새 중복이 생길 수 있으므로 반영 직전 쓰기 제어·재검증 방법을 결정한다.
4. 배포 직전 최신 Flyway 버전을 다시 확인한다. 다른 마이그레이션이 V28 이상을 먼저 가져갔다면 번호를 조정한다. 기존 적용 버전은 수정하지 않는다.
5. 1~3을 마친 뒤 `V28`이 포함된 릴리스를 배포한다. 기본 PostgreSQL Flyway 트랜잭션으로 실행된다. 1~3 전에는 이 릴리스를 main에 올리지 않는다.
6. 제약 추가 후 순차/동시 중복 요청이409이고, 다른 사용자·모집폼의 정상 지원은 성공하는지 확인한다.

사전 점검 SQL은 마이그레이션으로 등록하지 않는다. 직접 수동 실행이 승인된 경우에는 마이그레이션 SQL을 하나의 트랜잭션으로 실행한다.
제약 생성 중 기존 중복이 발견되면 실패·롤백된다. 자동 데이터 삭제나 `flyway repair`를 수행하지 않는다.
이 변경은 UNIQUE만 추가한다. 컬럼·기존 데이터·다른 제약을 삭제하거나 변경하지 않는다. NULL 허용 컬럼을 NOT NULL로 변경하는 작업은 별도 검토 대상이다.

현재 작업은 SQL 작성 및 전용 테스트 DB 검증이며 운영 DB 적용은 아니다.

## 전용 PostgreSQL 검증

기존 #426 전용 `ttokttok-426-repro-pg` 컨테이너를 시작한 뒤 다음을 실행한다.

```powershell
python deploy/test/test-applicant-unique-migration.py
```

임시 스키마에서 실제 SQL 파일을 실행해 제약명·중복 차단·다른 조합의 정상 저장·기존 중복 시 실패·사전 조회를 확인한다. 성공/실패 테스트의 스키마와 데이터는 트랜잭션 롤백으로 제거한다. 운영 DB를 사용하지 않는다.

## #432 club_members UNIQUE(club_id, email) 적용 순서

합격 확정과 수동 부원 등록이 동아리 행 잠금으로 중복 등록을 막는다(코드, #432). 이 제약은 잠금을 잡지 않는 쓰기 경로가 생겨도 중복이 저장되지 않게 하는 DB 백스톱이며, 실험 근거는 `load-test/results/issue-432/MEASUREMENTS.md`.
제약 SQL은 #443에서 `src/main/resources/db/migration/V29__add_club_member_club_email_unique_constraint.sql`로 옮겼다. 이 폴더에는 읽기 전용 사전 점검 `check_club_member_duplicates.sql`이 남는다.

1. 동아리 행 잠금 코드(#432)를 먼저 배포하고 이전 코드의 종료를 확인한다. 이전 코드가 남아 있는 동안 제약을 추가하면 이전 코드의 경쟁 요청이 500이 된다.
2. `check_club_member_duplicates.sql`로 중복·NULL·기존 제약/인덱스를 읽기 전용 점검한다. 중복이 있으면 부원 데이터를 자동 삭제하지 않고, 정리 방안을 별도로 승인받는다. 대소문자만 다른 이메일은 UNIQUE가 막지 못하므로 결과를 별도로 검토한다.
3. 테이블 잠금과 작업 시간을 검토한다. 사전 조회 이후 새 중복이 생길 수 있으므로 반영 직전 재확인 방법을 정한다.
4. 배포 직전 최신 Flyway 버전을 다시 확인한다. 다른 마이그레이션이 V29 이상을 먼저 가져갔다면 번호를 조정한다. 1~3을 마친 뒤 `V29`가 포함된 릴리스를 배포한다.
5. 제약 추가 후 합격 확정 반복·동시 요청이 같은 결과를 돌려주고 중복 부원이 없는지, 수동 등록의 중복 이메일이 기존처럼 409인지 확인한다.

```powershell
python deploy/test/test-club-member-unique-migration.py
```

## #438 구조 개선 전 운영 상태 점검

이메일 참조의 `user_id` 전환과 UNIQUE 추가 전에 운영 상태를 확인한다. `check_structure_integrity.sql`은 읽기 전용이며 자동 실행되지 않는다. 사용자 정보는 건수로만 조회한다.

운영 호스트의 `deploy/` 디렉터리에서 실행한다. 접속 정보는 컨테이너 환경 변수를 쓰고 명령에 직접 적지 않는다.

```bash
docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < migrations/pending/check_structure_integrity.sql > structure-check.txt

docker compose exec -T postgres sh -c 'pg_dump --schema-only --no-owner --no-privileges -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  > schema-baseline.sql
```

- `structure-check.txt`의 1~4번이 모두 0이어야 스키마 변경을 진행한다. 0이 아닌 항목은 데이터를 자동으로 고치지 않고 처리 방법을 먼저 정한다.
- `fcm_token.email`에 사용자가 아닌 값이 있으면 `fcm_token`을 `user_id` 전환 대상에서 제외한다.
- `schema-baseline.sql`은 데이터 없이 스키마만 담는다. 기준 스키마 테스트의 입력으로 쓴다.
