package org.project.ttokttok.domain.applyform.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ActiveApplyFormNotFoundException extends CustomException {
    public ActiveApplyFormNotFoundException() {
        super(ErrorMessage.ACTIVE_APPLY_FORM_NOT_FOUND);
    }
}
