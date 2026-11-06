package org.project.ttokttok.domain.club.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class NoApplyFormExistException extends CustomException {
    public NoApplyFormExistException() {
        super(ErrorMessage.NO_APPLY_FORM_EXIST);
    }
}
