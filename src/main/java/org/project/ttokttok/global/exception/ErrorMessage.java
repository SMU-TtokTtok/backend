package org.project.ttokttok.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    /* 에러 메시지와 HTTP 상태 반환하는 열거형.
    *  꼭 어떤 예외 인지 주석으로 파트를 구분하여 작성 및 추가 할 것.
    * */

    // 사용자 에러 메시지
    USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    //관리자 에러메시지
    ADMIN_NOT_FOUND("관리자를 찾을 수 없음.", HttpStatus.NOT_FOUND),
    ADMIN_PASSWORD_NOT_MATCH("비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_ADMIN("잘못된 관리자명입니다.", HttpStatus.NOT_FOUND),

    //토큰 에러 메시지
    INVALID_TOKEN_ISSUER("유효하지 않은 토큰 발급자입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("토큰이 만료되었거나, 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_EXISTS("리프레시 토큰이 이미 존재합니다.(이미 로그인한 사용자입니다.)", HttpStatus.CONFLICT),
    INVALID_ROLE("잘못된 역할 값이 토큰에 존재합니다.", HttpStatus.UNAUTHORIZED),
    ALREADY_LOGOUT("이미 로그아웃하였거나, 존재하지 않는 토큰입니다.", HttpStatus.CONFLICT),
    INVALID_TOKEN_AT_COOKIE("쿠키 측 리프레시 토큰이 Null입니다.", HttpStatus.BAD_REQUEST),
    INVALID_REFRESH_TOKEN("잘못된 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
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
    APPLY_FORM_INVALID_DATE_RANGE("시작 날짜는 종료 날짜보다 이전이어야 합니다.", HttpStatus.BAD_REQUEST),
    ACTIVE_APPLY_FORM_NOT_FOUND("활성화된 지원 폼을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 지원자 에러 메시지
    APPLICANT_NOT_FOUND("지원자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_APPLICANT_ACCESS("지원자 조회에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 메모 에러 메시지
    MEMO_NOT_FOUND("메모를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 즐겨찾기 에러 메시지
    FAVORITE_NOT_FOUND("즐겨찾기를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 부원 에러 메시지
    MEMBER_NOT_FOUND("부원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_ROLE("이미 해당 역할을 가진 부원이 존재합니다.", HttpStatus.CONFLICT),
    EXCEL_FILE_CREATE_FAIL("엑셀 파일 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ALREADY_CLUB_MEMBER("이미 동아리에 가입된 사용자입니다.", HttpStatus.CONFLICT),
    LIST_SIZE_NOT_MATCH("지원서 질문 ID 리스트와 파일 리스트의 크기는 같아야 합니다.", HttpStatus.BAD_REQUEST),
    ANSWER_REQUEST_NOT_MATCH("지원서 질문 ID와 답변 요청, 어느 한쪽이 Null 입니다.", HttpStatus.BAD_REQUEST),

    // S3 에러 메시지
    S3_FILE_UPLOAD_ERROR("S3 파일 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus status;
}
