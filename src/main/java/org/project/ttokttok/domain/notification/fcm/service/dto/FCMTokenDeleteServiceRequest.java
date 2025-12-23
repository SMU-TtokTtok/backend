package org.project.ttokttok.domain.notification.fcm.service.dto;

import lombok.Builder;

@Builder
public record FCMTokenDeleteServiceRequest(
        String email,
        String token
) {
}
