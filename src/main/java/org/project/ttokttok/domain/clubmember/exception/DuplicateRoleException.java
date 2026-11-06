package org.project.ttokttok.domain.clubmember.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class DuplicateRoleException extends CustomException {
    public DuplicateRoleException() {
        super(ErrorMessage.DUPLICATE_ROLE);
    }
}
