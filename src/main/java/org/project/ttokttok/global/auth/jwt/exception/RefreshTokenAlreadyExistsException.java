package org.project.ttokttok.global.auth.jwt.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class RefreshTokenAlreadyExistsException extends CustomException {
    public RefreshTokenAlreadyExistsException() {
        super(ErrorMessage.REFRESH_TOKEN_EXISTS);
    }
}
