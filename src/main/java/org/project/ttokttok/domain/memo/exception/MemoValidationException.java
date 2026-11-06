package org.project.ttokttok.domain.memo.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class MemoValidationException extends CustomException {
    public MemoValidationException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
