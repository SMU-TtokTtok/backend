package org.project.ttokttok.domain.admin.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

/**
 * 비밀번호 재설정 시 새 비밀번호와 확인 비밀번호가 일치하지 않는 경우
 */
public class AdminPasswordConfirmNotMatchException extends CustomException {
    public AdminPasswordConfirmNotMatchException() {
        super(ErrorMessage.ADMIN_PASSWORD_CONFIRM_NOT_MATCH);
    }
}
