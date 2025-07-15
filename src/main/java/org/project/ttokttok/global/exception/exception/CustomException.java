package org.project.ttokttok.global.exception.exception;

import lombok.Getter;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.springframework.http.HttpStatus;

@Getter
public abstract class CustomException extends RuntimeException {

    // 커스텀 익셉션 클래스 ->  커스텀 예외가 필요할 시, 상속받아 이용한다.
    // 상속받은 구현 클래스에는 생성자로 super(ErrorMessage.열거형 내용) 으로 설정
    private final HttpStatus status;

    public CustomException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        status = errorMessage.getStatus();
    }
}
