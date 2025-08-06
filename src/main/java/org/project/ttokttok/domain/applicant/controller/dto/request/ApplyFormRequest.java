package org.project.ttokttok.domain.applicant.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;

import java.util.List;

/**
 * 지원서 제출 요청 - JSON 형태로 받는 기본 정보와 답변들
 */
public record ApplyFormRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotNull(message = "나이는 필수입니다.")
        Integer age,

        @NotBlank(message = "전공은 필수입니다.")
        String major,

        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "전화번호는 필수입니다.")
        String phone,

        @NotNull(message = "학적 상태는 필수입니다.")
        StudentStatus studentStatus,

        @NotNull(message = "학년은 필수입니다.")
        Grade grade,

        @NotNull(message = "성별은 필수입니다.")
        Gender gender,

        @NotBlank(message = "지원폼 ID는 필수입니다.")
        String applyFormId,

        /**
         * 지원폼의 질문에 대한 답변들
         * 파일이 아닌 텍스트 답변들만 포함
         */
        @Valid
        List<AnswerRequest> answers
) {
}
