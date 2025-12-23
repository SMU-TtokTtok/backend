package org.project.ttokttok.domain.temp.applicant.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.controller.dto.request.AnswerRequest;

import java.util.List;

/**
 * 임시 지원서 저장 요청
 */
public record TempApplicantSaveRequest(
        @NotBlank(message = "지원폼 ID는 필수입니다.")
        String formId,

        String name,
        Integer age,
        String major,
        String email,
        String phone,
        StudentStatus studentStatus,
        Grade grade,
        Gender gender,

        /**
         * 임시 저장된 답변들 (작성되지 않은 필드는 null 허용)
         */
        @Valid
        List<AnswerRequest> answers
) {
}
