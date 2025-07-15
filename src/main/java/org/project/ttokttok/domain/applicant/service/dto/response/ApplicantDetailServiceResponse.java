package org.project.ttokttok.domain.applicant.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;

import java.util.List;

@Builder
public record ApplicantDetailServiceResponse(
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

    public static ApplicantDetailServiceResponse of(String name,
                                                    Integer age,
                                                    String major,
                                                    String email,
                                                    String phone,
                                                    StudentStatus studentStatus,
                                                    Grade grade,
                                                    Gender gender,
                                                    List<Answer> answers,
                                                    List<MemoResponse> memos) {

        return ApplicantDetailServiceResponse.builder()
                .name(name)
                .age(age)
                .major(major)
                .email(email)
                .phone(phone)
                .studentStatus(studentStatus)
                .grade(grade)
                .gender(gender)
                .answers(answers)
                .memos(memos)
                .build();
    }
}