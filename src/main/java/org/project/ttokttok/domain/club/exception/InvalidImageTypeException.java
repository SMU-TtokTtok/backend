package org.project.ttokttok.domain.club.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class InvalidImageTypeException extends CustomException {
    public InvalidImageTypeException() {
        super(ErrorMessage.INVALID_IMAGE_TYPE);
    }
}
