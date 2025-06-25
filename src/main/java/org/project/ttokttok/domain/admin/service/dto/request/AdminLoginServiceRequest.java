package org.project.ttokttok.domain.admin.service.dto.request;

import lombok.Builder;

@Builder
public record AdminLoginServiceRequest(
        String username,
        String password
) {
}
