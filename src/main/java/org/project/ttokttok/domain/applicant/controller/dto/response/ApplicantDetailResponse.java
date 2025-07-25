package org.project.ttokttok.domain.applicant.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantDetailServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.MemoResponse;

import java.util.List;

@Builder
public record ApplicantDetailResponse(
        String name,
        Integer age,
        String major,
        String email,
        String phone,
        StudentStatus studentStatus,
        Grade grade,
        Gender gender,
        List<Answer> answers,
        List<MemoResponse> memos
) {
    public static ApplicantDetailResponse from(ApplicantDetailServiceResponse response) {
        return ApplicantDetailResponse.builder()
                .name(response.name())
                .age(response.age())
                .major(response.major())
                .email(response.email())
                .phone(response.phone())
                .studentStatus(response.studentStatus())
                .grade(response.grade())
                .gender(response.gender())
                .answers(response.answers())
                .memos(response.memos())
                .build();
    }
}
