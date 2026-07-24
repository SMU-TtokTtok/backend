package org.project.ttokttok.domain.user.domain.enums;

/**
 * 사용자 계정의 인증 제공자
 * LOCAL: 이메일+비밀번호 가입, GOOGLE: Google OAuth 가입/연동
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
