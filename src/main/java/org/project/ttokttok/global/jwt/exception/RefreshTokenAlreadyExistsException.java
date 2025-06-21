package org.project.ttokttok.global.jwt.exception;

import org.project.ttokttok.global.error.ErrorMessage;
import org.project.ttokttok.global.error.exception.CustomException;

public class RefreshTokenAlreadyExistsException extends CustomException {
    public RefreshTokenAlreadyExistsException() {
        super(ErrorMessage.REFRESH_TOKEN_EXISTS);
    }
}
