package org.project.ttokttok.domain.clubboard.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ClubBoardNotFoundException extends CustomException {
    public ClubBoardNotFoundException() {
        super(ErrorMessage.CLUB_BOARD_NOT_FOUND);
    }
}
