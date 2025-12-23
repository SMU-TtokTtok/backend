package org.project.ttokttok.infrastructure.firebase.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;

@Builder
public record FCMRequest(
        @Nullable
        List<String> tokens,

        @NotBlank(message = "알림 제목이 비어있습니다.")
        String title,

        @NotBlank(message = "알림 본문이 비어있습니다.")
        String body
) {
}
