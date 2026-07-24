package org.project.ttokttok.domain.user.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.project.ttokttok.domain.user.service.dto.response.GoogleLoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "구글 로그인 응답 (needsOnboarding 값에 따라 로그인 완료 또는 온보딩 필요)")
public record GoogleLoginResponse(
        @Schema(description = "온보딩(약관 동의) 필요 여부", example = "false")
        boolean needsOnboarding,

        @Schema(description = "액세스 토큰 (로그인 완료 시)")
        String accessToken,

        @Schema(description = "리프레시 토큰 (로그인 완료 시)")
        String refreshToken,

        @Schema(description = "사용자 정보 (로그인 완료 시)")
        UserResponse user,

        @Schema(description = "온보딩 토큰 (온보딩 필요 시, 10분 유효)")
        String onboardingToken,

        @Schema(description = "구글 계정 이메일 (온보딩 필요 시)")
        String email,

        @Schema(description = "구글 프로필 이름 제안값 (온보딩 필요 시, 없을 수 있음)")
        String suggestedName
) {
    public static GoogleLoginResponse from(final GoogleLoginServiceResponse serviceResponse) {
        UserServiceResponse serviceUser = serviceResponse.user();

        return GoogleLoginResponse.builder()
                .needsOnboarding(serviceResponse.needsOnboarding())
                .accessToken(serviceResponse.accessToken())
                .refreshToken(serviceResponse.refreshToken())
                .user(serviceUser == null ? null : UserResponse.from(serviceUser))
                .onboardingToken(serviceResponse.onboardingToken())
                .email(serviceResponse.email())
                .suggestedName(serviceResponse.suggestedName())
                .build();
    }
}
