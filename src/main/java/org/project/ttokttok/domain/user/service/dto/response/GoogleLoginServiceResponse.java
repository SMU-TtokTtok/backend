package org.project.ttokttok.domain.user.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;

/**
 * 구글 로그인 서비스 응답
 * needsOnboarding=false: 로그인 완료 (토큰 + 사용자 정보)
 * needsOnboarding=true: 신규 사용자 - 약관 동의 필요 (온보딩 토큰 + 구글 프로필 힌트)
 */
@Builder
public record GoogleLoginServiceResponse(
        boolean needsOnboarding,
        String accessToken,
        String refreshToken,
        UserServiceResponse user,
        String onboardingToken,
        String email,
        String suggestedName
) {
    public static GoogleLoginServiceResponse ofLogin(final TokenResponse tokens, final UserServiceResponse user) {
        return GoogleLoginServiceResponse.builder()
                .needsOnboarding(false)
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .user(user)
                .build();
    }

    public static GoogleLoginServiceResponse ofOnboarding(final String onboardingToken,
                                                          final String email,
                                                          final String suggestedName) {
        return GoogleLoginServiceResponse.builder()
                .needsOnboarding(true)
                .onboardingToken(onboardingToken)
                .email(email)
                .suggestedName(suggestedName)
                .build();
    }
}
