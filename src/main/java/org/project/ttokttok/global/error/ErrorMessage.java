package org.project.ttokttok.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    //관리자 에러메시지
    ADMIN_NOT_FOUND("관리자를 찾을 수 없음.", HttpStatus.NOT_FOUND),
    ADMIN_PASSWORD_NOT_MATCH("비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED),

    //토큰 에러 메시지
    INVALID_TOKEN_ISSUER("유효하지 않은 토큰 발급자입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("토큰이 만료되었거나, 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_EXISTS("리프레시 토큰이 이미 존재합니다.", HttpStatus.CONFLICT);

    private final String message;
    private final HttpStatus status;
}
