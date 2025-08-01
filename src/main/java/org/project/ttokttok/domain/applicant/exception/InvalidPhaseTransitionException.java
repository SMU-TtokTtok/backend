package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidPhaseTransitionException extends CustomException {
    public InvalidPhaseTransitionException() {
        super(ErrorMessage.INVALID_PHASE_TRANSITION);
    }
}
