package org.project.ttokttok.infrastructure.s3.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class S3FileUploadException extends CustomException {
    public S3FileUploadException() {
        super(ErrorMessage.S3_FILE_UPLOAD_ERROR);
    }
}
