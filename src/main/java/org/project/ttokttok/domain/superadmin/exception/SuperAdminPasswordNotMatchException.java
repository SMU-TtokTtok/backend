package org.project.ttokttok.domain.superadmin.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class SuperAdminPasswordNotMatchException extends CustomException {
    public SuperAdminPasswordNotMatchException() {
        super(ErrorMessage.SUPER_ADMIN_PASSWORD_NOT_MATCH);
    }
}
