package org.project.ttokttok.domain.club.controller.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Set;

public record UpdateClubContentRequest(
        //@Size(min = 1, max = 50)
        //@NotBlank(message = "동아리 이름은 비어 있을 수 없습니다.")
        JsonNullable<String> name,

        //@NotBlank(message = "동아리 유형은 비어 있을 수 없습니다.")
        JsonNullable<ClubType> clubType,

        //@NotBlank(message = "동아리 카테고리는 비어 있을 수 없습니다.")
        JsonNullable<ClubCategory> clubCategory,

        //@Size(max = 30)
        //@NotNull(message = "커스텀 카테고리는 Null일 수 없습니다.")
        JsonNullable<String> customCategory,

        //@Size(max = 255)
        //@NotNull(message = "한줄 소개는 Null일 수 없습니다.")
        JsonNullable<String> summary,

        JsonNullable<MultipartFile> profileImage,

        //@Size(max = 6000)
        //@NotBlank(message = "소개글 내용은 비어 있을 수 없습니다.")
        JsonNullable<String> content,

        //@NotNull(message = "모집 여부는 Null일 수 없습니다.")
        JsonNullable<Boolean> recruiting,
        JsonNullable<LocalDate> applyStartDate,
        JsonNullable<LocalDate> applyEndDate,
        JsonNullable<Set<ApplicableGrade>> grades,
        JsonNullable<Integer> maxApplyCount
) {
    public ClubContentUpdateServiceRequest toServiceRequest(String clubId) {
        return ClubContentUpdateServiceRequest.builder()
                .clubId(clubId)
                .name(name)
                .clubType(clubType)
                .clubCategory(clubCategory)
                .customCategory(customCategory)
                .summary(summary)
                .profileImage(profileImage)
                .content(content)
                // .recruiting(recruiting) - ApplyForm에서 관리하므로 제거
                .applyStartDate(applyStartDate)
                .applyEndDate(applyEndDate)
                .grades(grades)
                .maxApplyCount(maxApplyCount)
                .build();
    }
}
