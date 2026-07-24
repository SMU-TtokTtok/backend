package org.project.ttokttok.domain.user.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class OnboardingAlreadyCompletedException extends CustomException {
    public OnboardingAlreadyCompletedException() {
        super(ErrorMessage.ONBOARDING_ALREADY_COMPLETED);
    }
}
