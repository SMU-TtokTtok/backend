package org.project.ttokttok.global.auth.jwt.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class RefreshTokenExpiredException extends CustomException {
    public RefreshTokenExpiredException() {
        super(ErrorMessage.REFRESH_TOKEN_EXPIRED);
    }
}
