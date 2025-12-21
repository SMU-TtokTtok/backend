package org.project.ttokttok.domain.notification.fcm.service.dto;

import lombok.Builder;

@Builder
public record FCMTokenSaveServiceRequest(
        String email,
        String token,
        String deviceType
) {
}
