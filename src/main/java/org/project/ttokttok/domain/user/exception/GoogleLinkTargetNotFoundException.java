package org.project.ttokttok.domain.user.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

/**
 * 구글 계정 자동 연동 대상 사용자를 찾지 못했을 때 발생한다.
 *
 * 트랜잭션 밖에서 이메일로 대상을 확인한 뒤 연동 트랜잭션 안에서 재조회하는 사이에
 * 계정이 삭제된 경쟁 상황이다. 재시도로 해소되는 성격이라 404 가 아니라 409 로 응답한다.
 */
public class GoogleLinkTargetNotFoundException extends CustomException {
    public GoogleLinkTargetNotFoundException() {
        super(ErrorMessage.GOOGLE_LINK_TARGET_NOT_FOUND);
    }
}
