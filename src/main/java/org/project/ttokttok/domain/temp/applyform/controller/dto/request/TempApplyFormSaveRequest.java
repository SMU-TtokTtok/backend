package org.project.ttokttok.domain.temp.applyform.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.json.Question;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 임시 지원폼 저장 요청
 */
public record TempApplyFormSaveRequest(
        @NotBlank(message = "동아리 ID는 필수입니다.")
        String clubId,

        String title,
        String subTitle,

        LocalDate applyStartDate,
        LocalDate applyEndDate,

        Boolean hasInterview,
        LocalDate interviewStartDate,
        LocalDate interviewEndDate,

        Integer maxApplyCount,

        /**
         * 지원 가능한 학년 목록 (임시저장에서는 선택사항)
         */
        Set<ApplicableGrade> grades,

        /**
         * 임시 저장된 질문들 (작성되지 않은 필드는 null 허용)
         */
        List<Question> formJson
) {
}
