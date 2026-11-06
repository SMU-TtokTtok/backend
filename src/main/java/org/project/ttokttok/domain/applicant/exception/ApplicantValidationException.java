package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class ApplicantValidationException extends CustomException {
    public ApplicantValidationException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
