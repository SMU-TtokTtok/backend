package org.project.ttokttok.domain.user.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "구글 로그인 요청")
public record GoogleLoginRequest(
        @Schema(description = "구글 ID 토큰 (Google Identity Services 발급)", example = "eyJhbGciOiJSUzI1NiIs...")
        @NotBlank(message = "구글 ID 토큰은 필수입니다.")
        String idToken
) {
}
