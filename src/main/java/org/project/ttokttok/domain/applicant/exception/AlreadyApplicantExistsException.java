package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class AlreadyApplicantExistsException extends CustomException {
    public AlreadyApplicantExistsException() {
        super(ErrorMessage.ALREADY_APPLICANT_EXISTS);
    }
}
