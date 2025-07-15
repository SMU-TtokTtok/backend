package org.project.ttokttok.domain.club.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class NotClubAdminException extends CustomException {
    public NotClubAdminException() {
        super(ErrorMessage.NOT_CLUB_ADMIN);
    }
}
