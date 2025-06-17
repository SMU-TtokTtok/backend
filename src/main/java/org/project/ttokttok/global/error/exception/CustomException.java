package org.project.ttokttok.global.error.exception;

import lombok.Getter;
import org.project.ttokttok.global.error.ErrorMessage;

@Getter
public abstract class CustomException extends RuntimeException {

    private final int statusCode;

    public CustomException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        statusCode = errorMessage.getStatus().value();
    }
}
