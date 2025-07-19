package org.project.ttokttok.domain.club.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record ClubDetailServiceResponse(
        String name,
        ClubType clubType,
        ClubCategory clubCategory,
        String customCategory,
        boolean bookmarked, // 사용자 즐겨찾기 여부
        boolean recruiting,
        String summary, // 한줄 소개
        String profileImageUrl, // 동아리 프로필 이미지 URL,
        int clubMemberCount,

        LocalDateTime applyStartDate,
        LocalDateTime applyDeadLine,
        Set<ApplicableGrade> grades, // 지원 가능한 학년
        int maxApplyCount, // 최대 지원자 수

        String content // 동아리 소개 내용
) {
    public static ClubDetailServiceResponse from(ClubDetailQueryResponse response) {
        return ClubDetailServiceResponse.builder()
                .name(response.name())
                .clubType(response.clubType())
                .clubCategory(response.clubCategory())
                .customCategory(response.customCategory())
                .bookmarked(response.bookmarked())
                .recruiting(response.recruiting())
                .summary(response.summary())
                .profileImageUrl(response.profileImageUrl())
                .clubMemberCount(response.clubMemberCount())
                .applyStartDate(response.applyStartDate() != null ? response.applyStartDate().atStartOfDay() : null)
                .applyDeadLine(response.applyDeadLine() != null ? response.applyDeadLine().atStartOfDay() : null)
                .grades(response.grades())
                .maxApplyCount(response.maxApplyCount())
                .content(response.content())
                .build();
    }
}
