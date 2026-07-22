package org.project.ttokttok.domain.user.service.dto.request;

import lombok.Builder;

@Builder
public record GoogleOnboardingCompleteServiceRequest(
        String onboardingToken,
        boolean termsAgreed,
        String name
) {
    public static GoogleOnboardingCompleteServiceRequest of(final String onboardingToken,
                                                            final boolean termsAgreed,
                                                            final String name) {
        return GoogleOnboardingCompleteServiceRequest.builder()
                .onboardingToken(onboardingToken)
                .termsAgreed(termsAgreed)
                .name(name)
                .build();
    }
}
