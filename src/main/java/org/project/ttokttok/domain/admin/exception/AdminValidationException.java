package org.project.ttokttok.domain.admin.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class AdminValidationException extends CustomException {
    public AdminValidationException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
