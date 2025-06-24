package org.project.ttokttok.global.error.exception;

import lombok.Getter;
import org.project.ttokttok.global.error.ErrorMessage;
import org.springframework.http.HttpStatus;

@Getter
public abstract class CustomException extends RuntimeException {

    private final HttpStatus status;

    public CustomException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        status = errorMessage.getStatus();
    }
}
