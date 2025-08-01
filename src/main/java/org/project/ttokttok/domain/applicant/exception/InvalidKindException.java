package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidKindException extends CustomException {
    public InvalidKindException() {
        super(ErrorMessage.INVALID_KIND);
    }
}
