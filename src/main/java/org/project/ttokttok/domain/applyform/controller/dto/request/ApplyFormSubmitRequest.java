package org.project.ttokttok.domain.applyform.controller.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record ApplyFormSubmitRequest(
        @NotBlank
        String name,

        @NotNull
        Integer age,

        @NotBlank
        String major,

        @Email
        String email,

        @NotBlank
        String phoneNumber,

        @NotNull
        StudentStatus studentStatus, // {ENROLLED, ABSENCE}

        @NotNull
        Grade grade, // 예: {FIRST, SECOND, THIRD, FOURTH}

        @NotNull
        Gender gender, // 예: {MALE, FEMALE}

        @NotNull
        List<QuestionAnswerRequest> answers
) {
}

record QuestionAnswerRequest(
        @NotBlank
        String questionId,

        @Nullable
        Object answer
) {
}
