package org.project.ttokttok.domain.user.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class GoogleAccountConflictException extends CustomException {
    public GoogleAccountConflictException() {
        super(ErrorMessage.GOOGLE_ACCOUNT_CONFLICT);
    }
}
