package org.project.ttokttok.domain.superadmin.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class SuperAdminNotFoundException extends CustomException {
    public SuperAdminNotFoundException() {
        super(ErrorMessage.SUPER_ADMIN_NOT_FOUND);
    }
}
