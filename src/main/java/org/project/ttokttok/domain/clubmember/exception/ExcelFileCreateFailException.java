package org.project.ttokttok.domain.clubmember.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class ExcelFileCreateFailException extends CustomException {
    public ExcelFileCreateFailException() {
        super(ErrorMessage.EXCEL_FILE_CREATE_FAIL);
    }
}
