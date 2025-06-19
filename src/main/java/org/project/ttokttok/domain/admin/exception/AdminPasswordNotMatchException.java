package org.project.ttokttok.domain.admin.exception;

import org.project.ttokttok.global.error.ErrorMessage;
import org.project.ttokttok.global.error.exception.CustomException;

public class AdminPasswordNotMatchException extends CustomException {
    public AdminPasswordNotMatchException() {
        super(ErrorMessage.ADMIN_PASSWORD_NOT_MATCH);
    }
}
