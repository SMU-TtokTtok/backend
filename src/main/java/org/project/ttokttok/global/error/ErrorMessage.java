package org.project.ttokttok.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {
    //에러메시지 저장용 열거형
    example("예시 에러 메시지", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus status;
}
