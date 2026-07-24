package org.project.ttokttok.global.auth.oauth.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class GoogleEmailNotVerifiedException extends CustomException {
    public GoogleEmailNotVerifiedException() {
        super(ErrorMessage.GOOGLE_EMAIL_NOT_VERIFIED);
    }
}
