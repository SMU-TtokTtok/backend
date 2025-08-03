package org.project.ttokttok.domain.applicant.service.dto.response;

import org.project.ttokttok.domain.applicant.domain.dto.ApplicantPageDto;

import java.util.List;

public record ApplicantPageServiceResponse(
        boolean hasInterview,
        int currentPage,
        int totalPage,
        int totalCount,
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
}
