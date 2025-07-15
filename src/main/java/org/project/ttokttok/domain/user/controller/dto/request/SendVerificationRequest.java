package org.project.ttokttok.domain.user.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerificationRequest(
        @NotBlank(message="이메일이 비어 있습니다.")
        @Schema(description = "상명대학교 이메일 주소", example = "202021000@sangmyung.kr")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email
) {
}
