package org.project.ttokttok.domain.superadmin.service.dto.request;

import lombok.Builder;

@Builder
public record SuperAdminLoginServiceRequest(
        String username,
        String password
) {
}
