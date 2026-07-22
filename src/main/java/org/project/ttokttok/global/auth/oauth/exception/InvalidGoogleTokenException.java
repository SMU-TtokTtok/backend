package org.project.ttokttok.global.auth.oauth.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidGoogleTokenException extends CustomException {
    public InvalidGoogleTokenException() {
        super(ErrorMessage.INVALID_GOOGLE_TOKEN);
    }
}
