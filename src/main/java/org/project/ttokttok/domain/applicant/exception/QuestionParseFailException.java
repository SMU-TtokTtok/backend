package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class QuestionParseFailException extends CustomException {
    public QuestionParseFailException() {
        super(ErrorMessage.QUESTION_PARSE_FAIL);
    }
}
