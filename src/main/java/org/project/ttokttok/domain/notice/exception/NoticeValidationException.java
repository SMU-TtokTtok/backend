package org.project.ttokttok.domain.notice.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class NoticeValidationException extends CustomException {
    public NoticeValidationException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
