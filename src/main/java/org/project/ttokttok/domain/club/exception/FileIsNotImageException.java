package org.project.ttokttok.domain.club.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class FileIsNotImageException extends CustomException {
    public FileIsNotImageException() {
        super(ErrorMessage.FILE_IS_NOT_IMAGE);
    }
}
