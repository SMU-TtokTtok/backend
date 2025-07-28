package org.project.ttokttok.domain.club.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailAdminQueryResponse;

import java.time.LocalDate;
import java.util.Set;

@Builder
public record ClubDetailAdminServiceResponse(
        String name,
        ClubType clubType,
        ClubCategory clubCategory,
        String customCategory,
        boolean recruiting,
        String summary,
        String profileImageUrl,
        int clubMemberCount,
        ClubUniv clubUniv,

        LocalDate applyStartDate,
        LocalDate applyDeadLine,
        Set<ApplicableGrade> grades,
        int maxApplyCount,

        String content
) {
    public static ClubDetailAdminServiceResponse from(ClubDetailAdminQueryResponse response) {
        return ClubDetailAdminServiceResponse.builder()
                .name(response.name())
                .clubType(response.clubType())
                .clubCategory(response.clubCategory())
                .customCategory(response.customCategory())
                .recruiting(response.recruiting())
                .summary(response.summary())
                .profileImageUrl(response.profileImageUrl())
                .clubMemberCount(response.clubMemberCount())
                .clubUniv(response.clubUniv())
                .applyStartDate(response.applyStartDate())
                .applyDeadLine(response.applyDeadLine())
                .grades(response.grades())
                .maxApplyCount(response.maxApplyCount())
                .content(response.content())
                .build();
    }
}
