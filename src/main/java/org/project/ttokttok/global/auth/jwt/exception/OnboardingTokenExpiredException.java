package org.project.ttokttok.global.auth.jwt.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class OnboardingTokenExpiredException extends CustomException {
    public OnboardingTokenExpiredException() {
        super(ErrorMessage.ONBOARDING_TOKEN_EXPIRED);
    }
}
