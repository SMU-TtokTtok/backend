package org.project.ttokttok.infrastructure.s3.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class S3ContentValidationException extends CustomException {
    public S3ContentValidationException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
