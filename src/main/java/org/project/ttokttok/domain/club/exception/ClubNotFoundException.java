package org.project.ttokttok.domain.club.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ClubNotFoundException extends CustomException {
    public ClubNotFoundException() {
        super(ErrorMessage.CLUB_NOT_FOUND);
    }
}
