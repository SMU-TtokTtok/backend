package org.project.ttokttok.domain.club.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailAdminServiceResponse;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record ClubAdminDetailResponse(
        // 동아리 소개 글 관리자용 응답 DTO
        String name,
        ClubType clubType,
        ClubCategory clubCategory,
        String customCategory,
        boolean bookmarked, // 사용자 즐겨찾기 여부
        boolean recruiting,
        String summary, // 한줄 소개
        String profileImageUrl, // 동아리 프로필 이미지 URL
        int clubMemberCount,
        ClubUniv clubUniv, // 대학 구분

        LocalDateTime applyStartDate,
        LocalDateTime applyDeadLine,
        Set<ApplicableGrade> grades, // 지원 가능한 학년
        int maxApplyCount, // 최대 지원자 수

        String content // 동아리 소개 내용
) {

    public static ClubAdminDetailResponse from(ClubDetailAdminServiceResponse response) {
        return ClubAdminDetailResponse.builder()
                .name(response.name())
                .clubType(response.clubType())
                .clubCategory(response.clubCategory())
                .customCategory(response.customCategory())
                .bookmarked(response.bookmarked())
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
