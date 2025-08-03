package org.project.ttokttok.domain.applicant.service.dto.response;

import org.project.ttokttok.domain.applicant.domain.dto.ApplicantPageDto;

import java.util.List;

public record ApplicantPageServiceResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ApplicantSimpleResponse> applicants
) {
    public static ApplicantPageServiceResponse from(ApplicantPageDto dto) {
        return new ApplicantPageServiceResponse(
                dto.currentPage(),
                dto.totalPage(),
                dto.totalCount(),
                dto.applicants()
        );
    }
}
