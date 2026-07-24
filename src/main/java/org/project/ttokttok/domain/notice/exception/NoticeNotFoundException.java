package org.project.ttokttok.domain.notice.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class NoticeNotFoundException extends CustomException {
    public NoticeNotFoundException() {
        super(ErrorMessage.NOTICE_NOT_FOUND);
    }
}
