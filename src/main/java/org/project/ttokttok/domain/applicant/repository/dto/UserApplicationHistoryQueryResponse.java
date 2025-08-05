package org.project.ttokttok.domain.applicant.repository.dto;

import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;

import java.time.LocalDateTime;

/**
 * 사용자 지원내역 조회용 Repository DTO
 * 사용자가 지원한 동아리 정보와 지원 상태를 포함합니다.
 */
public record UserApplicationHistoryQueryResponse(
        String applicantId,
        String clubId,
        String clubName,
        ClubType clubType,
        ClubCategory clubCategory,
        String customCategory,
        String summary,
        String profileImageUrl,
        int clubMemberCount,
        boolean recruiting,
        boolean bookmarked,
        ApplicantPhase currentPhase,
        LocalDateTime appliedAt
) {
}