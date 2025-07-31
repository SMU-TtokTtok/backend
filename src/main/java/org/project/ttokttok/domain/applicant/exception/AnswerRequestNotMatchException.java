package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class AnswerRequestNotMatchException extends CustomException {
    public AnswerRequestNotMatchException() {
        super(ErrorMessage.ANSWER_REQUEST_NOT_MATCH);
    }
}
