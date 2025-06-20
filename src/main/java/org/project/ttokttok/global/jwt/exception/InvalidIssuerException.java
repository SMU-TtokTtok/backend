package org.project.ttokttok.global.jwt.exception;

import org.project.ttokttok.global.error.ErrorMessage;
import org.project.ttokttok.global.error.exception.CustomException;

public class InvalidIssuerException extends CustomException {
    public InvalidIssuerException() {
        super(ErrorMessage.INVALID_TOKEN_ISSUER);
    }
}
