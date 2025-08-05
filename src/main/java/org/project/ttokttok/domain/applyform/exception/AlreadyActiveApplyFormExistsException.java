package org.project.ttokttok.domain.applyform.exception;

import org.project.ttokttok.global.exception.ErrorMessage;

public class AlreadyActiveApplyFormExistsException extends RuntimeException {
    public AlreadyActiveApplyFormExistsException() {
        super(ErrorMessage.ALREADY_ACTIVE_APPLY_FORM_EXISTS);
    }
}
