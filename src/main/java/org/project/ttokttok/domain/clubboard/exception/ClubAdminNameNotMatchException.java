package org.project.ttokttok.domain.clubboard.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ClubAdminNameNotMatchException extends CustomException {
    public ClubAdminNameNotMatchException() {
        super(ErrorMessage.ADMIN_NAME_NOT_MATCH);
    }
}
