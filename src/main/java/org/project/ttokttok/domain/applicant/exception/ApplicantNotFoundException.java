package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ApplicantNotFoundException extends CustomException {

    public ApplicantNotFoundException() {
        super(ErrorMessage.APPLICANT_NOT_FOUND);
    }
}
