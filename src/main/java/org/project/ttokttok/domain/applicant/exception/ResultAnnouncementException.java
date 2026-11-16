package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.CustomException;
import org.project.ttokttok.global.exception.ErrorMessage;

public class ResultAnnouncementException extends CustomException {

    public ResultAnnouncementException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
