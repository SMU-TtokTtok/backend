package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class NoInterviewPhaseException extends CustomException {
    public NoInterviewPhaseException() {
        super(ErrorMessage.NO_INTERVIEW_PHASE);
    }
}
