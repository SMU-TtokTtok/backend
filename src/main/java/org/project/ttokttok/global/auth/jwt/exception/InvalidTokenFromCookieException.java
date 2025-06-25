package org.project.ttokttok.global.auth.jwt.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidTokenFromCookieException extends CustomException {
    public InvalidTokenFromCookieException() {
        super(ErrorMessage.INVALID_TOKEN_AT_COOKIE);
    }
}
