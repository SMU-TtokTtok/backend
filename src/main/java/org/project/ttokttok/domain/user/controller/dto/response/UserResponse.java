package org.project.ttokttok.domain.user.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;

@Builder
public record UserResponse(
        String id,
        String email,
        String name,
        boolean isEmailVerified,
        boolean termsAgreed
) {
    public static UserResponse from(final UserServiceResponse serviceResponse) {
        return UserResponse.builder()
                .id(serviceResponse.id())
                .email(serviceResponse.email())
                .name(serviceResponse.name())
                .isEmailVerified(serviceResponse.isEmailVerified())
                .termsAgreed(serviceResponse.termsAgreed())
                .build();
    }
}
