package org.project.ttokttok.infrastructure.s3.exception;

import static org.project.ttokttok.global.exception.ErrorMessage.S3_FILE_SIZE_TOO_LARGE;

import org.project.ttokttok.global.exception.exception.CustomException;

public class S3FileMaxSizeOverException extends CustomException {
    public S3FileMaxSizeOverException() {
        super(S3_FILE_SIZE_TOO_LARGE);
    }
}
