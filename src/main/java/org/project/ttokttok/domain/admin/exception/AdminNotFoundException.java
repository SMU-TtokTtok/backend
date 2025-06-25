package org.project.ttokttok.domain.admin.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class AdminNotFoundException extends CustomException {
    public AdminNotFoundException() {
        super(ErrorMessage.ADMIN_NOT_FOUND);
    }
}
