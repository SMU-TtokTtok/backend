package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class UnAuthorizedApplicantAccessException extends CustomException {
    public UnAuthorizedApplicantAccessException() {
        super(ErrorMessage.UNAUTHORIZED_APPLICANT_ACCESS);
    }
}