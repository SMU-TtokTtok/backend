package org.project.ttokttok.global.auth.oauth.dto;

import lombok.Builder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 구글 ID 토큰 검증 후 추출한 사용자 정보
 *
 * @param sub 구글 불변 식별자 (이메일은 변경될 수 있으므로 sub 로 식별)
 * @param email 구글 계정 이메일
 * @param emailVerified 구글 측 이메일 검증 여부 (false 면 자동 연동 금지 - 계정 탈취 방지)
 * @param name 구글 프로필 이름 (없을 수 있음)
 */
@Builder
public record GoogleUserInfo(
        String sub,
        String email,
        boolean emailVerified,
        String name
) {
    public static GoogleUserInfo from(final Jwt jwt) {
        return GoogleUserInfo.builder()
                .sub(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .emailVerified(Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")))
                .name(jwt.getClaimAsString("name"))
                .build();
    }
}
