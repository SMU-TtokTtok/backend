package org.project.ttokttok.domain.user.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.project.ttokttok.domain.user.service.dto.request.GoogleOnboardingCompleteServiceRequest;

@Schema(description = "구글 온보딩 완료 요청 (약관 동의 + 이름 입력)")
public record GoogleOnboardingCompleteRequest(
        @Schema(description = "구글 로그인 응답으로 받은 온보딩 토큰 (10분 유효)")
        @NotBlank(message = "온보딩 토큰은 필수입니다.")
        String onboardingToken,

        @Schema(description = "약관 동의 여부 (true 필수)", example = "true")
        boolean termsAgreed,

        @Schema(description = "사용자 이름 (2~10자)", example = "김철수")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 10, message = "이름은 2자 이상 10자 이하여야 합니다.")
        String name
) {
    public GoogleOnboardingCompleteServiceRequest toServiceRequest() {
        return GoogleOnboardingCompleteServiceRequest.of(onboardingToken, termsAgreed, name);
    }
}
