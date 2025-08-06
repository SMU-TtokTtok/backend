package org.project.ttokttok.domain.applicant.service.dto.response;

import org.project.ttokttok.domain.applicant.domain.dto.ApplicantPageDto;

import java.util.List;

public record ApplicantPageServiceResponse(
        Boolean hasInterview,
        Integer currentPage,
        Integer totalPage,
        Integer totalCount,
        List<ApplicantSimpleResponse> applicants
) {
    public static ApplicantPageServiceResponse of(ApplicantPageDto dto, boolean hasInterview) {
        return new ApplicantPageServiceResponse(
                hasInterview,
                dto.currentPage(),
                dto.totalPage(),
                dto.totalCount(),
                dto.applicants()
        );
    }

    // 지원 폼이 존재하지 않을 경우 빈 응답을 반환하는 메서드
    public static ApplicantPageServiceResponse toEmpty() {
        return new ApplicantPageServiceResponse(
                null,
                null,
                null,
                null,
                null
        );
    }
}
