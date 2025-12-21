package org.project.ttokttok.domain.notification.fcm.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FCMTokenSaveRequest(
        @NotBlank(message = "FCM 토큰 입력이 비어 있음.")
        String token,

        @NotBlank(message = "기기 종류가 비어 있음.")
        String deviceType
) {
}
