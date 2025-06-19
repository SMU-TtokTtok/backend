package org.project.ttokttok.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    //관리자 에러메시지
    ADMIN_NOT_FOUND("관리자를 찾을 수 없음.", HttpStatus.NOT_FOUND),
    ADMIN_PASSWORD_NOT_MATCH("비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED);

    private final String message;
    private final HttpStatus status;
}
