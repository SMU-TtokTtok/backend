package org.project.ttokttok.global.auth.jwt.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidIssuerException extends CustomException {
    public InvalidIssuerException() {
        super(ErrorMessage.INVALID_TOKEN_ISSUER);
    }
}
