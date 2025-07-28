package org.project.ttokttok.domain.admin.controller.dto.response;

public record AdminJoinResponse(
        String adminId
) {
    public static AdminJoinResponse of(final String adminId) {
        return new AdminJoinResponse(adminId);
    }
}
