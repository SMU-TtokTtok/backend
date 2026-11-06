package org.project.ttokttok.infrastructure.email.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class EmailValidationException extends CustomException {
    public EmailValidationException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
