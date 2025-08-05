package org.project.ttokttok.domain.applyform.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class AlreadyActiveApplyFormExistsException extends CustomException {
    public AlreadyActiveApplyFormExistsException() {
        super(ErrorMessage.ALREADY_ACTIVE_APPLY_FORM_EXISTS);
    }
}
