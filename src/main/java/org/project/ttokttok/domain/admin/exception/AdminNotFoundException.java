package org.project.ttokttok.domain.admin.exception;

import org.project.ttokttok.global.error.ErrorMessage;
import org.project.ttokttok.global.error.exception.CustomException;

public class AdminNotFoundException extends CustomException {
    public AdminNotFoundException() {
        super(ErrorMessage.ADMIN_NOT_FOUND);
    }
}
