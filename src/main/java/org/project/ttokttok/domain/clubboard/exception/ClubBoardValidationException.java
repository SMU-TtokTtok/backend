package org.project.ttokttok.domain.clubboard.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ClubBoardValidationException extends CustomException {
    public ClubBoardValidationException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
