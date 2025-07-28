package org.project.ttokttok.domain.club.service.dto.response;

import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;

import java.time.LocalDateTime;
import java.util.Set;

public record ClubDetailAdminServiceResponse(
        String name,
        ClubType clubType,
        ClubCategory clubCategory,
        String customCategory,
        boolean bookmarked,
        boolean recruiting,
        String summary,
        String profileImageUrl,
        int clubMemberCount,
        ClubUniv clubUniv,

        LocalDateTime applyStartDate,
        LocalDateTime applyDeadLine,
        Set<ApplicableGrade> grades,
        int maxApplyCount,

        String content
) {
}
