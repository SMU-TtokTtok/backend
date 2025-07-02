package org.project.ttokttok.domain.user.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.user.domain.User;

@Builder
public record UserServiceResponse(
        String id,
        String email,
        String name,
        boolean isEmailVerified,
        boolean termsAgreed
) {
    public static UserServiceResponse from(final User user) {
        return UserServiceResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .isEmailVerified(user.isEmailVerified())
                .termsAgreed(user.isTermsAgreed())
                .build();
    }
}
