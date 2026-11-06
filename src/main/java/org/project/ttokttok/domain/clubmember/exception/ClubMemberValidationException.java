package org.project.ttokttok.domain.clubmember.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class ClubMemberValidationException extends CustomException {
    public ClubMemberValidationException(String roleName) {
        super(ErrorMessage.INVALID_REGISTRATION_ROLE, ErrorMessage.INVALID_REGISTRATION_ROLE.getMessage() + roleName);
    }
}
