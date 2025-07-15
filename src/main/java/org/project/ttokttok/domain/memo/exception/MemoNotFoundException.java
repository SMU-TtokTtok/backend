package org.project.ttokttok.domain.memo.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class MemoNotFoundException extends CustomException {
    public MemoNotFoundException() {
        super(ErrorMessage.MEMO_NOT_FOUND);
    }
}

