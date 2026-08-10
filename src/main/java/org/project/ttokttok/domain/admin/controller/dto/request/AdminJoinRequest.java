package org.project.ttokttok.domain.admin.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.project.ttokttok.domain.admin.service.dto.request.AdminJoinServiceRequest;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;

public record AdminJoinRequest(
        @NotBlank(message = "아이디가 비어 있습니다.")
        @Size(min = 8, message = "아이디는 최소 8글자여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호가 비어 있습니다.")
        @Size(min = 12, message = "비밀번호는 최소 12글자여야 합니다.")
        String password,

        @NotBlank(message = "동아리 명이 비어있습니다.")
        String clubName,

        @NotNull(message = "동아리 학부가 비어있습니다.")
        ClubUniv clubUniv
) {
    public AdminJoinServiceRequest toServiceRequest() {
        return AdminJoinServiceRequest.builder()
                .username(username)
                .password(password)
                .clubName(clubName)
                .clubUniv(clubUniv)
                .build();
    }
}
