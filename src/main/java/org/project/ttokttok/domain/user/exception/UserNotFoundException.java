package org.project.ttokttok.domain.user.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class UserNotFoundException extends CustomException {
    public UserNotFoundException() {
        super(ErrorMessage.USER_NOT_FOUND);
    }
}
