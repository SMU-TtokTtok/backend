package org.project.ttokttok.domain.club.repository.dto;

import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;

import java.time.LocalDate;
import java.util.Set;

public record ClubDetailAdminQueryResponse(
        String name,
        ClubType clubType,
        ClubCategory clubCategory,
        String customCategory,
        boolean recruiting,
        String summary, // 한줄 소개
        String profileImageUrl, // 동아리 프로필 이미지 URL,
        int clubMemberCount,
        ClubUniv clubUniv, // 동아리 소속 대학교

        LocalDate applyStartDate,    // LocalDate로 변경
        LocalDate applyDeadLine,     // LocalDate로 변경
        Set<ApplicableGrade> grades, // 지원 가능한 학년
        int maxApplyCount, // 최대 지원자 수

        String content // 동아리 소개 내용
) {
}
