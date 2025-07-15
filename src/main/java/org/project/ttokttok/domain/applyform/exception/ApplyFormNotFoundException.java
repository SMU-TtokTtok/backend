package org.project.ttokttok.domain.applyform.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ApplyFormNotFoundException extends CustomException {
    public ApplyFormNotFoundException() {
        super(ErrorMessage.APPLY_FORM_NOT_FOUND);
    }
}
