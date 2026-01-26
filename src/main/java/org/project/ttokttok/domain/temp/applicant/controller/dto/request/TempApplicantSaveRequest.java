package org.project.ttokttok.domain.temp.applicant.controller.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.project.ttokttok.domain.applicant.controller.dto.request.AnswerRequest;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;

/**
 * 임시 지원서 저장 요청
 */
public record TempApplicantSaveRequest(
        @Nullable String name,
        @Nullable Integer age,
        @Nullable String major,
        @Nullable String email,
        @Nullable String phone,
        @Nullable StudentStatus studentStatus,
        @Nullable Grade grade,
        @Nullable Gender gender,

        /**
         * 지원폼의 질문에 대한 답변들
         * 파일이 아닌 텍스트 답변들만 포함
         */
        List<TempAnswer> answers
) {
}
