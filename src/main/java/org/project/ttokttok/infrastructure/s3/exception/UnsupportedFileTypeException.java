package org.project.ttokttok.infrastructure.s3.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class UnsupportedFileTypeException extends CustomException {
    public UnsupportedFileTypeException() {
        super(ErrorMessage.S3_UNSUPPORTED_FILE_TYPE);
    }
}
