package org.project.ttokttok.domain.user.service.dto.request;

import lombok.Builder;

@Builder
public record LoginServiceRequest(
        String email,
        String password,
        boolean rememberMe  // 로그인 유지 옵션
) {
}
