package org.project.ttokttok.domain.admin.service.dto.request;

import lombok.Builder;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;

@Builder
public record AdminJoinServiceRequest(
        String username,
        String password,
        String email,
        String clubName,
        ClubUniv clubUniv
) {
}
