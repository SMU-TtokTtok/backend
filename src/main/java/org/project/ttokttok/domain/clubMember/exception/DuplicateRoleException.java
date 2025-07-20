package org.project.ttokttok.domain.clubMember.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class DuplicateRoleException extends CustomException {
    public DuplicateRoleException() {
        super(ErrorMessage.DUPLICATE_ROLE);
    }
}
