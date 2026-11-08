package org.project.ttokttok.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    // Validation messages preserve the existing HTTP 400 contracts.
    USER_TERMS_REQUIRED("약관 동의가 필요합니다.", HttpStatus.BAD_REQUEST),
    GOOGLE_SUB_REQUIRED("구글 식별자(sub)는 필수입니다.", HttpStatus.BAD_REQUEST),
    USER_EMAIL_REQUIRED("이메일은 필수입니다.", HttpStatus.BAD_REQUEST),
    USER_EMAIL_INVALID("올바른 이메일 형식이 아닙니다.", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_REQUIRED("비밀번호는 필수입니다.", HttpStatus.BAD_REQUEST),
    USER_NAME_REQUIRED("이름은 필수입니다.", HttpStatus.BAD_REQUEST),
    USER_NAME_TOO_SHORT("이름은 최소 2자 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_CONFIRM_MISMATCH("비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    USER_EMAIL_ALREADY_REGISTERED("이미 가입된 이메일입니다.", HttpStatus.BAD_REQUEST),
    USER_AUTH_NOT_FOUND("존재하지 않는 사용자입니다.", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_MISMATCH("비밀번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    USER_ACCOUNT_NOT_VERIFIED("이메일 인증이 완료되지 않은 계정입니다.", HttpStatus.BAD_REQUEST),
    USER_NEW_PASSWORD_MISMATCH("새 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    VERIFICATION_EMAIL_INVALID("상명대학교 이메일만 사용 가능합니다.", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_INVALID("올바르지 않은 인증코드입니다.", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_EXPIRED("인증코드가 만료되었습니다.", HttpStatus.BAD_REQUEST),
    VERIFICATION_EMAIL_NOT_VERIFIED("이메일 인증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST),
    VERIFICATION_RECORD_NOT_FOUND("인증 코드 성공 여부가 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
    ADMIN_USERNAME_REQUIRED("관리자 명은 null이거나 비어있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    ADMIN_USERNAME_LENGTH_INVALID("관리자 명은 8자 이상 20자 이하여야 합니다.", HttpStatus.BAD_REQUEST),
    ADMIN_USERNAME_CHARACTERS_INVALID("관리자 명은 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.", HttpStatus.BAD_REQUEST),
    ADMIN_PASSWORD_REQUIRED("비밀번호는 null이거나 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    MEMO_CREATE_DOCUMENT_REQUIRED("메모는 서류 지원자에만 작성할 수 있습니다.", HttpStatus.BAD_REQUEST),
    MEMO_UPDATE_DOCUMENT_REQUIRED("메모는 서류 지원자에만 수정할 수 있습니다.", HttpStatus.BAD_REQUEST),
    MEMO_DELETE_DOCUMENT_REQUIRED("메모는 서류 지원자에만 삭제할 수 있습니다.", HttpStatus.BAD_REQUEST),
    MEMO_CONTENT_BLANK("메모 내용은 비워둘 수 없습니다.", HttpStatus.BAD_REQUEST),
    NOTICE_TITLE_BLANK("Title cannot be null or blank.", HttpStatus.BAD_REQUEST),
    NOTICE_CONTENT_BLANK("Content cannot be null or blank.", HttpStatus.BAD_REQUEST),
    CLUB_BOARD_CLUB_REQUIRED("동아리 정보가 없습니다.", HttpStatus.BAD_REQUEST),
    CLUB_BOARD_TITLE_BLANK("제목은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    CLUB_BOARD_IMAGE_REQUIRED("대표 이미지가 없습니다.", HttpStatus.BAD_REQUEST),
    APPLICANT_ALREADY_INTERVIEW("이미 면접 단계에 존재하는 지원자입니다.", HttpStatus.BAD_REQUEST),
    REQUIRED_ANSWER_MISSING("필수 질문에 대한 답변이 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_APPLICABLE_GRADE("잘못된 학년입니다.", HttpStatus.BAD_REQUEST),
    INVALID_REGISTRATION_ROLE("잘못된 역할명입니다: ", HttpStatus.BAD_REQUEST),
    EMAIL_SIGNUP_DOMAIN_INVALID("상명대학교 이메일만 가입 가능합니다.", HttpStatus.BAD_REQUEST),
    S3_CONTENT_EMPTY("파일이 비어있거나 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
    S3_FILE_NAME_EMPTY("파일 이름이 비어있습니다.", HttpStatus.BAD_REQUEST),
    S3_FILE_NAME_CHARACTERS_INVALID("파일 이름에 허용되지 않는 특수 문자가 포함되어 있습니다.", HttpStatus.BAD_REQUEST),
    S3_FILE_NAME_TOO_LONG("파일 이름이 너무 깁니다. (최대 255자)", HttpStatus.BAD_REQUEST),
    S3_CONTENT_URL_INVALID("삭제하려는 파일을 찾을 수 없음", HttpStatus.BAD_REQUEST),

    /* 에러 메시지와 HTTP 상태 반환하는 열거형.
    *  꼭 어떤 예외 인지 주석으로 파트를 구분하여 작성 및 추가 할 것.
    * */

    // 사용자 에러 메시지
    USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    //관리자 에러메시지
    ADMIN_NOT_FOUND("관리자를 찾을 수 없음.", HttpStatus.NOT_FOUND),
    // 로그인 시 관리자를 찾지 못한 경우. 잘못된 요청(아이디 오류)이므로 400으로 반환한다.
    ADMIN_LOGIN_NOT_FOUND("관리자를 찾을 수 없음.", HttpStatus.BAD_REQUEST),
    ADMIN_PASSWORD_NOT_MATCH("비밀번호가 틀렸습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ADMIN("잘못된 관리자명입니다.", HttpStatus.NOT_FOUND),
    ADMIN_ALREADY_EXIST_NAME("이미 존재하는 아이디입니다.", HttpStatus.CONFLICT),
    ADMIN_PASSWORD_CONFIRM_NOT_MATCH("새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

    //토큰 에러 메시지
    INVALID_TOKEN_ISSUER("유효하지 않은 토큰 발급자입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("토큰이 만료되었거나, 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_EXISTS("리프레시 토큰이 이미 존재합니다.(이미 로그인한 사용자입니다.)", HttpStatus.CONFLICT),
    INVALID_ROLE("잘못된 역할 값이 토큰에 존재합니다.", HttpStatus.UNAUTHORIZED),
    ALREADY_LOGOUT("이미 로그아웃하였거나, 존재하지 않는 토큰입니다.", HttpStatus.CONFLICT),
    INVALID_REFRESH_TOKEN("잘못된 리프레시 토큰입니다.", HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_EXPIRED("리프레시 토큰 유효 기간 만료, 다시 로그인 필요", HttpStatus.FORBIDDEN),

    //동아리 에러 메시지
    CLUB_NOT_FOUND("동아리를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_CLUB_ADMIN("해당 동아리의 관리자가 아닙니다.", HttpStatus.FORBIDDEN),
    FILE_IS_NOT_IMAGE("파일이 이미지가 아닙니다.", HttpStatus.BAD_REQUEST),
    NO_APPLY_FORM_EXIST("해당 동아리에 지원 폼이 존재하지 않습니다.", HttpStatus.NOT_FOUND),

    // 동아리 게시판 에러 메시지
    ADMIN_NAME_NOT_MATCH("요청한 관리자가 이 동아리의 관리자와 다릅니다.", HttpStatus.FORBIDDEN),
    CLUB_NULL_POINTER("클럽 객체가 Null입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CONTENT_NULL_OR_BLANK("내용이 Null 이거나 비어있습니다.", HttpStatus.BAD_REQUEST),
    TITLE_NULL_OR_BLANK("Title이 Null 이거나 비어있습니다.", HttpStatus.BAD_REQUEST),
    CLUB_BOARD_NOT_FOUND("게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 지원 폼 에러 메시지
    APPLY_FORM_NOT_FOUND("지원 폼을 찾을 수 없거나, 활성화된 지원 폼이 없습니다.", HttpStatus.NOT_FOUND),
    APPLY_FORM_INVALID_DATE_RANGE("잘못된 날짜 지정 범위 입니다.", HttpStatus.BAD_REQUEST),
    ACTIVE_APPLY_FORM_NOT_FOUND("활성화된 지원 폼을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_ACTIVE_APPLY_FORM_EXISTS("이미 활성화된 지원폼이 존재합니다.", HttpStatus.BAD_REQUEST),
    // 지원 폼 입력값 규칙 (400)
    RECRUIT_PERIOD_TOO_LONG("모집 기간은 최대 3개월까지 설정할 수 있습니다.", HttpStatus.BAD_REQUEST),
    APPLY_FORM_GRADES_REQUIRED("모집 대상 학년을 1개 이상 선택해 주세요.", HttpStatus.BAD_REQUEST),
    APPLY_FORM_QUESTIONS_REQUIRED("질문을 1개 이상 추가해 주세요.", HttpStatus.BAD_REQUEST),
    APPLY_FORM_MAX_COUNT_INVALID("모집 인원은 1명 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    INTERVIEW_DATE_REQUIRED("면접이 있는 폼은 면접 시작일과 종료일을 입력해 주세요.", HttpStatus.BAD_REQUEST),
    INTERVIEW_INVALID_DATE_RANGE("면접 시작일이 면접 종료일보다 늦을 수 없습니다.", HttpStatus.BAD_REQUEST),
    // 지원 폼 상태 규칙 (409)
    APPLY_FORM_NOT_STARTED("아직 모집 기간이 아닙니다.", HttpStatus.CONFLICT),
    APPLY_FORM_CLOSED("모집이 마감되었습니다.", HttpStatus.CONFLICT),
    TEMP_APPLY_NOT_OPEN("모집 기간이 아니라 임시저장할 수 없습니다.", HttpStatus.CONFLICT),
    RECRUIT_PERIOD_NOT_OPEN("오늘은 모집 기간이 아닙니다. 모집 기간을 먼저 수정해 주세요.", HttpStatus.CONFLICT),
    APPLY_FORM_EDIT_LOCKED("지원서가 제출된 폼이라 이 항목은 바꿀 수 없습니다.", HttpStatus.CONFLICT),
    APPLY_START_DATE_LOCKED("지원서가 제출된 폼은 시작일을 바꿀 수 없습니다.", HttpStatus.CONFLICT),
    FINISH_WHILE_RECRUITING("모집 중에는 지원자를 초기화할 수 없습니다. 먼저 지원을 마감해 주세요.", HttpStatus.CONFLICT),
    FINISH_UNLINKED_PASSERS("부원으로 연동되지 않은 최종 합격자가 있습니다. '최종 부원연동'을 먼저 눌러 주세요.", HttpStatus.CONFLICT),

    // 지원자 에러 메시지
    APPLICANT_NOT_FOUND("지원자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_APPLICANT_ACCESS("지원자 조회에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_PHASE_TRANSITION("지원자의 상태를 변경할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_KIND("지원자의 상태를 변경할 수 없습니다. 잘못된 kind 값입니다.", HttpStatus.BAD_REQUEST),
    INVALID_ANSWER("질문 규칙에 맞지 않는 답변입니다.", HttpStatus.BAD_REQUEST),
    LIST_SIZE_NOT_MATCH("지원서 질문 ID 리스트와 파일 리스트의 크기는 같아야 합니다.", HttpStatus.BAD_REQUEST),
    ANSWER_REQUEST_NOT_MATCH("지원서 질문 ID와 답변 요청, 어느 한쪽이 Null 입니다.", HttpStatus.BAD_REQUEST),
    INVALID_PHASE_STATUS("잘못된 STATUS 입력입니다.", HttpStatus.BAD_REQUEST),
    NO_INTERVIEW_PHASE("인터뷰 단계가 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_APPLICANT_EXISTS("이미 지원한 동아리입니다.", HttpStatus.CONFLICT),
    QUESTION_PARSE_FAIL("질문을 받아오는데 실패했습니다.", HttpStatus.BAD_REQUEST),

    // 메모 에러 메시지
    MEMO_NOT_FOUND("메모를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 즐겨찾기 에러 메시지
    FAVORITE_NOT_FOUND("즐겨찾기를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 부원 에러 메시지
    MEMBER_NOT_FOUND("부원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_ROLE("이미 해당 역할을 가진 부원이 존재합니다.", HttpStatus.CONFLICT),
    EXCEL_FILE_CREATE_FAIL("엑셀 파일 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ALREADY_CLUB_MEMBER("이미 동아리에 가입된 사용자입니다.", HttpStatus.CONFLICT),
    MEMBER_ACCESS_DENIED("해당 부원에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // S3 에러 메시지
    S3_FILE_UPLOAD_ERROR("S3 파일 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    S3_DELETE_NOT_FOUND("삭제하려는 파일을 찾을 수 없음", HttpStatus.NOT_FOUND),
    S3_FILE_SIZE_TOO_LARGE("파일 크기가 20MB를 초과할 수 없습니다.", HttpStatus.PAYLOAD_TOO_LARGE),
    S3_UNSUPPORTED_FILE_TYPE("지원하지 않는 파일 형식입니다. 이미지 또는 문서 파일만 허용됩니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    S3_ZIP_SLIP_ERROR("압축 파일 내 유효하지 않은 파일 경로가 포함되어 있습니다.", HttpStatus.BAD_REQUEST),
    S3_ZIP_FILE_COUNT_LIMIT("압축 파일 내 파일 개수가 너무 많습니다. (최대 50개)", HttpStatus.BAD_REQUEST),
    S3_ZIP_UNCOMPRESSED_SIZE_LIMIT("압축 해제 시 허용 용량을 초과합니다. (최대 100MB)", HttpStatus.PAYLOAD_TOO_LARGE),
    S3_ZIP_INTERNAL_FILE_TYPE_ERROR("압축 파일 내 허용되지 않는 파일 형식이 포함되어 있습니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // FCM 에러 메시지
    FCM_FIELD_BLANK("FCM 토큰에 저장할 필드가 비어 있습니다.", HttpStatus.BAD_REQUEST),
    FCM_EMAIL_BLANK("FCM 토큰 내 저장 시도된 이메일이 비어 있습니다.", HttpStatus.BAD_REQUEST),
    FCM_TOKEN_BLANK("FCM 토큰 내 저장 시도된 토큰 값이 비어 있습니다.", HttpStatus.BAD_REQUEST),

    // 운영자(슈퍼 관리자) 에러 메시지
    SUPER_ADMIN_NOT_FOUND("운영자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SUPER_ADMIN_PASSWORD_NOT_MATCH("비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED),

    // 공지사항 에러 메시지
    NOTICE_NOT_FOUND("공지사항을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 구글 OAuth 에러 메시지
    INVALID_GOOGLE_TOKEN("유효하지 않은 구글 토큰입니다.", HttpStatus.UNAUTHORIZED),
    GOOGLE_EMAIL_NOT_VERIFIED("구글 계정의 이메일 인증이 필요합니다.", HttpStatus.BAD_REQUEST),
    GOOGLE_ACCOUNT_CONFLICT("이미 다른 구글 계정과 연동된 이메일입니다.", HttpStatus.CONFLICT),
    OAUTH_ONLY_ACCOUNT("구글 로그인으로 가입된 계정입니다. 구글 로그인을 이용해주세요.", HttpStatus.CONFLICT),
    INVALID_ONBOARDING_TOKEN("유효하지 않은 가입 세션입니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED),
    ONBOARDING_TOKEN_EXPIRED("가입 세션이 만료되었습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED),
    ONBOARDING_ALREADY_COMPLETED("이미 처리된 가입 요청입니다.", HttpStatus.CONFLICT),
    GOOGLE_LINK_TARGET_NOT_FOUND("계정 상태가 변경되었습니다. 다시 로그인해주세요.", HttpStatus.CONFLICT);

    private final String message;
    private final HttpStatus status;
}
