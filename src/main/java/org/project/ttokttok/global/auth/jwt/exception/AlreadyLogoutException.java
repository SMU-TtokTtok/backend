package org.project.ttokttok.global.auth.jwt.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class AlreadyLogoutException extends CustomException {
    public AlreadyLogoutException() {
        super(ErrorMessage.ALREADY_LOGOUT);
    }
}
