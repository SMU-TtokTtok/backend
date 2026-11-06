package org.project.ttokttok.domain.applyform.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class ApplyFormValidationException extends CustomException {
    public ApplyFormValidationException(int grade) {
        super(ErrorMessage.INVALID_APPLICABLE_GRADE, ErrorMessage.INVALID_APPLICABLE_GRADE.getMessage() + grade);
    }
}
