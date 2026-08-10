package org.project.ttokttok.domain.admin.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

/**
 * 로그인 시 관리자(또는 소속 동아리)를 찾을 수 없는 경우.
 * reset-password, JWT 인증 필터에서 쓰이는 {@link AdminNotFoundException}(404)과 달리,
 * 로그인 API는 잘못된 요청으로 간주해 400을 반환한다.
 */
public class AdminLoginNotFoundException extends CustomException {
    public AdminLoginNotFoundException() {
        super(ErrorMessage.ADMIN_LOGIN_NOT_FOUND);
    }
}
