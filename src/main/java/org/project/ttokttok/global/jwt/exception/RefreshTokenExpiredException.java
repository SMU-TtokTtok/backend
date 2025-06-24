package org.project.ttokttok.global.jwt.exception;

import org.project.ttokttok.global.error.ErrorMessage;
import org.project.ttokttok.global.error.exception.CustomException;

public class RefreshTokenExpiredException extends CustomException {
    public RefreshTokenExpiredException() {
        super(ErrorMessage.REFRESH_TOKEN_EXPIRED);
    }
}
