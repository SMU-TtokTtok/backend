package org.project.ttokttok.domain.applyform.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidDateRangeException extends CustomException {
    public InvalidDateRangeException() {
        super(ErrorMessage.APPLY_FORM_INVALID_DATE_RANGE);
    }
}
