package org.project.ttokttok.domain.notification.fcm.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class FCMTokenValidationException extends CustomException {
    public FCMTokenValidationException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
