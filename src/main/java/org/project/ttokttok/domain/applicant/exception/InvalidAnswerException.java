package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidAnswerException extends CustomException {
    public InvalidAnswerException() {
        super(ErrorMessage.INVALID_ANSWER);
    }
}
