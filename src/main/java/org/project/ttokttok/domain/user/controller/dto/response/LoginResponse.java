package org.project.ttokttok.domain.user.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;

@Builder
public record LoginResponse(
        // String accessToken,     // 액세스 토큰 (헤더 대신 응답 바디에 포함)
        UserResponse user       // 사용자 정보
        // refreshToken은 쿠키로 전달하므로 응답에 포함하지 않음
) {
    public static LoginResponse from(final LoginServiceResponse serviceResponse) {
        return LoginResponse.builder()
                .user(UserResponse.from(serviceResponse.user()))
                .build();
    }
}
