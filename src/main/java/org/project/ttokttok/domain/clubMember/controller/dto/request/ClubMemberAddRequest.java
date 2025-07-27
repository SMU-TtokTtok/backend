package org.project.ttokttok.domain.clubMember.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberServiceRequest;

public record ClubMemberAddRequest(
        @NotNull(message = "학번은 필수 입력값입니다.")
        Long studentNum,

        @NotBlank(message = "이름은 필수 입력값입니다.")
        String name,

        @NotBlank(message = "전공은 필수 입력값입니다.")
        String major,

        @NotNull(message = "학년은 필수 입력값입니다.")
        Grade grade,

        @NotNull(message = "역할은 필수 입력값입니다.")
        MemberRole role,

        @Email(message = "유효하지 않은 이메일 형식입니다.")
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        String email,

        @NotBlank(message = "전화번호는 필수 입력값입니다.")
        String phoneNumber,

        @NotNull(message = "성별은 필수 입력값입니다.")
        Gender gender
) {
    public ClubMemberServiceRequest toServiceRequest() {
        return ClubMemberServiceRequest.builder()
                .studentNum(studentNum)
                .name(name)
                .major(major)
                .grade(grade)
                .role(role)
                .email(email)
                .gender(gender)
                .phoneNumber(phoneNumber)
                .build();
    }
}
