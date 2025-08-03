package org.project.ttokttok.domain.club.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Set;

@Schema(description = "동아리 소개 수정 요청 데이터")
public record UpdateClubContentRequest(
        @Schema(
                description = "동아리 이름",
                type = "string",
                example = "상명대학교 축구동아리",
                maxLength = 50
        )
        JsonNullable<String> name,

        @Schema(
                description = "동아리 유형",
                example = "CENTRAL",
                allowableValues = {"CENTRAL", "MAJOR", "COURSE"}
        )
        JsonNullable<ClubType> clubType,

        @Schema(
                description = "동아리 카테고리",
                type = "string",
                example = "SPORTS",
                allowableValues = {"ACADEMIC", "SPORTS", "ARTS", "HOBBY", "VOLUNTEER", "OTHER"}
        )
        JsonNullable<ClubCategory> clubCategory,

        @Schema(
                description = "대학 구분",
                type = "string",
                example = "ENGINEERING",
                allowableValues = {"GLOBAL_AREA", "DESIGN", "ENGINEERING", "CONVERGENCE_TECHNOLOGY", "ARTS"}
        )
        JsonNullable<ClubUniv> clubUniv,

        @Schema(
                description = "커스텀 카테고리명 (사용자 직접입력)",
                type = "string",
                example = "e스포츠",
                maxLength = 30
        )
        JsonNullable<String> customCategory,

        @Schema(
                description = "동아리 한줄 소개",
                type = "string",
                example = "축구를 사랑하는 사람들이 모인 동아리입니다!",
                maxLength = 255
        )
        JsonNullable<String> summary,

        @Schema(
                description = "동아리 상세 소개 (마크다운 형식 지원)",
                type = "string",
                example = "## 동아리 소개\n축구를 통해 친목을 도모하고...",
                maxLength = 6000
        )
        JsonNullable<String> content,

        @Schema(
                description = "모집 시작일",
                example = "2025-01-01",
                type = "string",
                format = "date"
        )
        JsonNullable<LocalDate> applyStartDate,

        @Schema(
                description = "모집 마감일",
                example = "2025-01-31",
                type = "string",
                format = "date"
        )
        JsonNullable<LocalDate> applyEndDate,

        @Schema(
                description = "모집 대상 학년",
                type = "array",
                example = "[\"FIRST_GRADE\", \"SECOND_GRADE\"]",
                allowableValues = {"FIRST_GRADE", "SECOND_GRADE", "THIRD_GRADE", "FOURTH_GRADE"}
        )
        JsonNullable<Set<ApplicableGrade>> grades,

        @Schema(
                description = "최대 모집 인원",
                type = "integer",
                example = "30",
                minimum = "1"
        )
        JsonNullable<Integer> maxApplyCount
) {
    public ClubContentUpdateServiceRequest toServiceRequest(String clubId, JsonNullable<MultipartFile> profileImage) {
        return ClubContentUpdateServiceRequest.builder()
                .clubId(clubId)
                .name(name)
                .clubType(clubType)
                .clubCategory(clubCategory)
                .clubUniv(clubUniv)
                .customCategory(customCategory)
                .summary(summary)
                .profileImage(profileImage)
                .content(content)
                .applyStartDate(applyStartDate)
                .applyEndDate(applyEndDate)
                .grades(grades)
                .maxApplyCount(maxApplyCount)
                .build();
    }
}
