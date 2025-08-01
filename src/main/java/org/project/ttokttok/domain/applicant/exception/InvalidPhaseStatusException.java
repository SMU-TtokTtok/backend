package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidPhaseStatusException extends CustomException {
    public InvalidPhaseStatusException() {
        super(ErrorMessage.INVALID_PHASE_STATUS);
    }
}
