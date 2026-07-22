package org.project.ttokttok.domain.user.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class OAuthOnlyAccountException extends CustomException {
    public OAuthOnlyAccountException() {
        super(ErrorMessage.OAUTH_ONLY_ACCOUNT);
    }
}
