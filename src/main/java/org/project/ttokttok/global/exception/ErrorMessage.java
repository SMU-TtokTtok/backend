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

    //관리자 에러메시지
    ADMIN_NOT_FOUND("관리자를 찾을 수 없음.", HttpStatus.NOT_FOUND),
    ADMIN_PASSWORD_NOT_MATCH("비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_ADMIN("잘못된 관리자명입니다.", HttpStatus.FORBIDDEN),

    //토큰 에러 메시지
    INVALID_TOKEN_ISSUER("유효하지 않은 토큰 발급자입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("토큰이 만료되었거나, 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_EXISTS("리프레시 토큰이 이미 존재합니다.(이미 로그인한 사용자입니다.)", HttpStatus.CONFLICT),
    INVALID_ROLE("잘못된 역할 값이 토큰에 존재합니다.", HttpStatus.UNAUTHORIZED),
    ALREADY_LOGOUT("이미 로그아웃하였거나, 존재하지 않는 토큰입니다.", HttpStatus.CONFLICT),
    INVALID_TOKEN_AT_COOKIE("쿠키 측 리프레시 토큰이 Null입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("잘못된 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("리프레시 토큰 유효 기간 만료, 다시 로그인 필요", HttpStatus.FORBIDDEN);

    private final String message;
    private final HttpStatus status;
}
