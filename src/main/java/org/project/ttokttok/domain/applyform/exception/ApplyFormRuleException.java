package org.project.ttokttok.domain.applyform.exception;

import org.project.ttokttok.global.exception.CustomException;
import org.project.ttokttok.global.exception.ErrorMessage;

/**
 * 지원 폼 규칙 위반. 입력값 오류는 400, 지금 상태에서 할 수 없는 요청은 409 로
 * {@link ErrorMessage} 에 정의한다.
 */
public class ApplyFormRuleException extends CustomException {

    public ApplyFormRuleException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public ApplyFormRuleException(ErrorMessage errorMessage, String message) {
        super(errorMessage, message);
    }
}
