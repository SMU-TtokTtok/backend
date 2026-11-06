# Day 2 잔여 구조 정리

- 유형: refactor / 이슈: 미등록. 공개 이슈·커밋·푸시·배포는 수행하지 않는다.
- 사용자 요청으로 순서 변경: Day 2 잔여 전체 완료 → #438 스키마 안전망. 이전 임시 지원폼 소유권 수정은 보존한다.
- API·DB·생산 의존성 변경 없음. 로컬 OpenAPI 스냅샷과 기존 오류 상태·메시지 보존.

## 검증 단위

1. 지원자 전형·정렬 enum: HTTP 형식을 유지하고 controller 이후 문자열을 제거. 전체 사용처·테스트 전환. 기존 서비스·Repository의 문자열 입력 테스트는 새 내부 타입에 맞춰 변경하고 잘못된 HTTP 값은 컨트롤러 테스트로 보장한다.
2. 업무 예외: 도메인별 명시적 예외로 교체하고 400·메시지 유지. 라이브러리 파싱·JSON 변환 예외는 제외.
3. 이름·패키지: 컨트롤러 ApiController, clubmember 소문자, 중복 exception 경로 정리. JPQL·테스트·설정 참조를 함께 이전.
4. 주석·TODO: 불필요한 코드 삭제, 별도 기능 변경이 필요한 항목은 구체적인 로컬 작업 명세로 이전.
5. editorconfig: 기존 들여쓰기 유지, Java 4칸, UTF-8·LF·최종 개행. 포매터·전체 재포맷 없음.

각 단위의 관련 테스트와 전체 clean build 통과 후 다음 단위를 시작한다. 최종 검증 수치와 한계는 IMPLEMENTATION.md에 기록한다.

## 완료 기록 (2026-11-06)

- enum, 업무 예외, 이름·패키지, 주석 정리 순으로 각각 전체 clean build 통과.
- 마지막 코드 변경에서 전체1093건, 실패/오류0, 스킵12. 로컬 API 스냅샷 동일, ArchUnit·권한·부원 JPQL 회귀 실행 확인.
- editorconfig 추가 후 파싱 및 Java4칸·Gradle탭·LF/배치CRLF self-check 통과. 전체 재포맷·의존성 변경 없음.
- main TODO/FIXME9건 정리: 불필요2건 제거, 별도 동작 변경7건은 `maintenance/tasks/day2-deferred-behavior.md`로 이전. 업무 구현 완료로 간주하지 않음.
- Windows의 case-only Git index 경로는 커밋 준비 시 명시적으로 기록해야 한다. 이번 작업에서는 index·커밋·원격·운영 상태를 변경하지 않음.
- Day2 완료 후 #438로 복귀. 현재 스키마 덤프·Flyway 이력 미확보로 PostgreSQL 스키마 검증은 대기 중.
