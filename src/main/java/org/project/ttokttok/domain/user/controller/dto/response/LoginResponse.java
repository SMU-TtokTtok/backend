package org.project.ttokttok.domain.user.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;

@Builder
public record LoginResponse(
        String accessToken,     // 액세스 토큰
        String refreshToken,    // 리프레시 토큰
        UserResponse user       // 사용자 정보
) {
    public static LoginResponse from(final LoginServiceResponse serviceResponse) {
        return LoginResponse.builder()
                .accessToken(serviceResponse.accessToken())
                .refreshToken(serviceResponse.refreshToken())
                .user(UserResponse.from(serviceResponse.user()))
                .build();
    }
}
