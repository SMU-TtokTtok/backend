package org.project.ttokttok.domain.applyform.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormCreateServiceRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

//todo: 검증 강화
public record ApplyFormCreateRequest(
        @NotNull(message = "면접 전형 여부는 Null일 수 없습니다.")
        boolean hasInterview,

        @NotNull(message = "지원서 작성 시작일은 Null일 수 없습니다.")
        LocalDate recruitStartDate,

        @NotNull(message = "지원서 작성 종료일은 Null일 수 없습니다.")
        LocalDate recruitEndDate,

        @NotNull(message = "지원 가능한 학년은 Null일 수 없습니다.")
        Set<Integer> applicableGrades,

        @NotNull(message = "최대 지원 횟수는 Null일 수 없습니다.")
        int maxApplyCount,

        @Nullable
        @Schema(description = "면접 시작일, 면접 전형이 없는 경우 Null일 수 있습니다.", example = "2000-01-02 혹은 null")
        LocalDate interviewStartDate,

        @Nullable
        @Schema(description = "면접 종료일, 면접 전형이 없는 경우 Null일 수 있습니다.", example = "2000-01-02 혹은 null")
        LocalDate interviewEndDate,

        @NotBlank(message = "지원서 제목은 비어 있을 수 없습니다.")
        String title,
        String subTitle,

        @NotNull(message = "지원 폼 내용은 Null일 수 없습니다.")
        List<Question> questions
) {
    public ApplyFormCreateServiceRequest toServiceRequest(String username, String clubId) {
        return ApplyFormCreateServiceRequest.builder()
                .username(username)
                .clubId(clubId)
                .hasInterview(hasInterview)
                .recruitStartDate(recruitStartDate)
                .recruitEndDate(recruitEndDate)
                .applicableGrades(applicableGrades)
                .maxApplyCount(maxApplyCount)
                .interviewStartDate(interviewStartDate)
                .interviewEndDate(interviewEndDate)
                .title(title)
                .subTitle(subTitle)
                .questions(questions)
                .build();
    }
}
