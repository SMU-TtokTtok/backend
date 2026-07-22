package org.project.ttokttok.global.auth.jwt.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidOnboardingTokenException extends CustomException {
    public InvalidOnboardingTokenException() {
        super(ErrorMessage.INVALID_ONBOARDING_TOKEN);
    }
}
