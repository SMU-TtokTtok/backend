package org.project.ttokttok.domain.club.controller.dto.response;

import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailServiceResponse;

import java.time.LocalDateTime;
import java.util.Set;

public record ClubDetailResponse(
        // 동아리 소개 글 응답 DTO

        String name,
        ClubType clubType,
        ClubCategory clubCategory,
        String customCategory,
        boolean bookmarked, // 사용자 즐겨찾기 여부
        boolean recruiting,
        String summary, // 한줄 소개
        String profileImageUrl, // 동아리 프로필 이미지 URL
        int clubMemberCount,

        LocalDateTime applyStartDate,
        LocalDateTime applyDeadLine,
        Set<ApplicableGrade> grades, // 지원 가능한 학년
        int maxApplyCount, // 최대 지원자 수

        String content // 동아리 소개 내용
) {
    public static ClubDetailResponse from(ClubDetailServiceResponse response) {
        return new ClubDetailResponse(
                response.name(),
                response.clubType(),
                response.clubCategory(),
                response.customCategory(),
                response.bookmarked(),
                response.recruiting(),
                response.summary(),
                response.profileImageUrl(),
                response.clubMemberCount(),
                response.applyStartDate(),
                response.applyDeadLine(),
                response.grades(),
                response.maxApplyCount(),
                response.content()
        );
    }
}
