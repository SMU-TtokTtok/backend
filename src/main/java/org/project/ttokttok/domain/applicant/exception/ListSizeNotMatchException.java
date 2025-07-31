package org.project.ttokttok.domain.applicant.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ListSizeNotMatchException extends CustomException {
    public ListSizeNotMatchException() {
        super(ErrorMessage.LIST_SIZE_NOT_MATCH);
    }
}
