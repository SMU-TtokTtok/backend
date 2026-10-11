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
