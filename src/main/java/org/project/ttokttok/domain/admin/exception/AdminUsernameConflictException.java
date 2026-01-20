package org.project.ttokttok.domain.admin.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class AdminUsernameConflictException extends CustomException {
    public AdminUsernameConflictException() {
        super(ErrorMessage.ADMIN_ALREADY_EXIST_NAME);
    }
}
