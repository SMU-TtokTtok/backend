package org.project.ttokttok.domain.admin.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class AdminEmailConflictException extends CustomException {
    public AdminEmailConflictException() {
        super(ErrorMessage.ADMIN_ALREADY_EXIST_EMAIL);
    }
}
