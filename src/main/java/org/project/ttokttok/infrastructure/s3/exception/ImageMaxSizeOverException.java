package org.project.ttokttok.infrastructure.s3.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ImageMaxSizeOverException extends CustomException {
    public ImageMaxSizeOverException() {
        super(ErrorMessage.IMAGE_MAX_SIZE_OVER);
    }
}
