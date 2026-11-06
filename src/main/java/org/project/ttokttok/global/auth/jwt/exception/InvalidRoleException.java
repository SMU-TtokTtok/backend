package org.project.ttokttok.global.auth.jwt.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class InvalidRoleException extends CustomException {
    public InvalidRoleException() {
        super(ErrorMessage.INVALID_ROLE);
    }
}
