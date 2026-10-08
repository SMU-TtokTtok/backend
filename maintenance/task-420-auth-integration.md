# #420 최종 인증·인가 통합 검증

## 작업 정의

- 유형: 테스트 보강 (Feature add & improve)
- 범위: club, clubMember, applicant, memo, clubboard의 HTTP → 보안 필터 → 서비스 → 도메인 → Repository 통합 검증
- 변경 대상: 동아리·부원·지원자·메모·게시판 컨트롤러별 통합 테스트, ClubAccessPolicyTest, IMPLEMENTATION.md
- DB 변경: 없음. 공개 API·오류 응답·의존성 변경 없음.

## 완료 기준

- [x] 실제 JwtFactory 토큰으로 다른 동아리 접근 시 HTTP 403과 statusCode/details 검증
- [x] 거부 후 flush/clear 및 DB 재조회로 대상 데이터 미변경 검증
- [x] 거부 시 EmailService, FCMService, S3Service 호출 0건 검증
- [x] 정상·거부 요청을 별도 테스트로 분리하고 해당 동아리 관리자 토큰의 성공과 DB 변경 검증
- [x] 부원/게시판의 요청 clubId와 리소스 소속 불일치 모두 검증
- [x] 면접 합격자 마감 시 부원 등록 및 결과 메일 수신자/내용 검증
- [x] 이미지 첨부 수정과 모집 시작/종료 및 FCM 수신자/내용 검증
- [x] 지원자 메모 생성/수정/삭제 검증
- [x] 각 컨트롤러에서 토큰 없는 대표 변경 요청 401 및 정책의 동일/다른 ID 검증
- [x] 전체 clean build 및 JaCoCo 게이트 통과
- [x] IMPLEMENTATION.md 기록

공통 통합 클래스나 부모 테스트 클래스 없이 컨트롤러별로 필요한 데이터만 준비한다.
기존 사용자 메모·게시판 변경은 보존한다. 최종 검증 후 사용자의 요청에 따라 커밋·푸시한다. PR 생성과 이슈 종료는 수행하지 않는다.
H2 검증이며 PostgreSQL 성능 및 N+1 분석은 범위에서 제외한다.

## 검증 결과

- 동아리 7개, 부원 7개, 지원자 9개, 메모 7개, 게시판 20개 컨트롤러 테스트 통과.
- 전체 977개 테스트: 실패 0, 오류 0, 스킵 0. `gradlew.bat clean build` 및 JaCoCo 통과.
- 보안 필터·서비스·Repository는 실제 구현으로 실행하고 메일·FCM·S3만 mock으로 대체했다.
- 요청 전후 flush/clear 및 DB 재조회로 저장 결과를 확인했다. 테스트 트랜잭션은 종료 시 롤백되므로 실제 커밋 후 S3 삭제 실행은 기존 별도 테스트에 맡긴다.
