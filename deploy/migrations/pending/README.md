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

### #438 운영 담당자 전달 자료 (2026-11-06)

위 명령은 이미 운영 읽기 작업이 승인된 담당자가 실행한다. DB 변경·마이그레이션 적용·데이터 정리는 하지 않는다.

#### 지금 스키마 검증에 필요한 최소 자료

Day 2 정리 후 #438을 재개할 때는 **스키마 덤프와 Flyway 이력** 두 파일이면 된다. 담당자는 운영 `deploy/`에서 아래 명령을 실행한다.

```bash
docker compose exec -T postgres sh -c 'pg_dump --schema-only --no-owner --no-privileges -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  > schema-baseline.sql

docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT installed_rank, version, description, type, success FROM flyway_schema_history ORDER BY installed_rank"' \
  > flyway-history.txt
```

실행 성공 여부를 확인한 뒤 두 파일을 비공개 로컬 폴더에 보관하고 그 경로를 전달한다. 실행 시각과 PostgreSQL 버전도 있으면 함께 기록한다. 운영 고아·중복 점검은 지금 테스트를 만드는 데 필수가 아니며, 해당 FK·UNIQUE·컬럼 전환을 운영에 적용하기 전에 실행한다.

#### 이후 운영 DB 변경 전 추가 점검

1. 앞의 `#438 구조 개선 전 운영 상태 점검` 절에 있는 명령으로 `structure-check.txt`를 만든다. 구조 점검의 5번 결과에 적용된 Flyway 버전 목록이 포함된다. 기존 스키마 덤프가 있다면 이 단계에서 다시 만들 필요는 없다.
2. 아래 명령으로 부원 중복 점검 결과도 저장한다.

```bash
docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < migrations/pending/check_club_member_duplicates.sql > club-member-check.txt
```

3. 실행 시각과 PostgreSQL 서버 버전을 함께 알려준다. 실패한 명령이 있으면 성공으로 간주하지 않고 해당 단계와 오류를 확인한다. 접속 정보·환경 변수·컨테이너 전체 로그는 전달하지 않는다.
4. 결과 파일은 비공개 로컬 경로에 보관하고 그 경로를 알려준다. 스키마만 있는 덤프에도 객체명·함수·주석이 포함될 수 있으므로 공개 이슈·PR·저장소에 원본을 올리지 않는다. 부원 점검 결과에 이메일이 포함되면 공유 전에 가리고 중복 그룹 수를 남긴다.

자료를 받은 뒤 테스트용 기준 스키마와 기준 Flyway 버전을 확정한다. 스키마 덤프의 이력 테이블은 데이터가 없으므로 현재 소스의 최신 버전을 임의로 기준점으로 쓰지 않는다. 전용 PostgreSQL에서 복원 → 확인된 버전으로 Flyway 기준점 설정 → 이후 마이그레이션 → Hibernate validate 순으로 검증한다.

## #438 기준 스키마 검증

운영 스키마 덤프를 전용 PostgreSQL에 복원하고, 아직 적용되지 않은 Flyway 마이그레이션을 실행한 뒤 `ddl-auto=validate`로 앱을 기동한다. 마이그레이션 실패와 엔티티·스키마 불일치를 운영 배포 전에 잡는다.

```powershell
docker start ttokttok-426-repro-pg
$port = (docker port ttokttok-426-repro-pg 5432).Split(':')[-1]
$env:TTOKTTOK_CONCURRENCY_JDBC_URL = "jdbc:postgresql://127.0.0.1:$port/ttokttok426"
$env:TTOKTTOK_SCHEMA_BASELINE_FILE = '<비공개 경로>\schema-baseline.sql'
$env:TTOKTTOK_SCHEMA_BASELINE_VERSION = '29'
.\gradlew.bat cleanTest test --tests '*SchemaBaselineValidationTest' -x jacocoTestCoverageVerification
```

- 덤프는 저장소에 넣지 않는다. `TTOKTTOK_SCHEMA_BASELINE_VERSION`은 덤프를 뜬 시점의 운영 Flyway 버전이다.
- 환경 변수가 없으면 건너뛰므로 `maintenance/verify.sh`만으로는 실행되지 않는다. **마이그레이션이나 엔티티를 바꾼 작업은 이 명령을 직접 실행한다.**
- 운영에 새 마이그레이션을 적용한 뒤에는 덤프와 버전을 함께 갱신한다. 덤프 시점 이후의 마이그레이션만 실행되기 때문이다.
- 음성 대조: 스키마에 없는 컬럼, 없는 테이블을 고치는 마이그레이션, 엔티티가 쓰는 컬럼을 지우는 마이그레이션이 모두 실패했고, 문제없는 마이그레이션은 적용됐다.
- 데이터는 없는 스키마 검증이다. 백필, 잠금 시간, 기존 데이터의 제약 위반은 잡지 못한다.
