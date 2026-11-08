package org.project.ttokttok.domain.club.repository.dto;

import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;

import java.time.LocalDate;

public record ClubCardQueryResponse(
        String id,
        String name,
        ClubType clubType,
        ClubCategory clubCategory,
        String customCategory,
        String summary,
        String profileImageUrl,
        int clubMemberCount,
        boolean recruiting,
        boolean upcoming,                  // 시작일 전인 활성 폼이 있음
        boolean bookmarked,
        LocalDate applyDeadLine  // applyDeadLine -> applyEndDate로 변경
) {
    public RecruitState recruitState() {
        if (recruiting) {
            return RecruitState.OPEN;
        }
        return upcoming ? RecruitState.UPCOMING : RecruitState.CLOSED;
    }
}
