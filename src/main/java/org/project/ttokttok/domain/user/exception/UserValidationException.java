package org.project.ttokttok.domain.user.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class UserValidationException extends CustomException {
    public UserValidationException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
