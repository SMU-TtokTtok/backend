package org.project.ttokttok.domain.notification.fcm.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FCMTokenDeleteRequest(
        @NotBlank(message = "FCM 토큰 입력이 비어있음.")
        String token
) {
}
