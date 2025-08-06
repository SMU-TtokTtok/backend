package org.project.ttokttok.domain.applicant.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.dto.ApplicantSimpleInfoDto;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantPageServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantSimpleResponse;

import java.util.List;

@Builder
public record ApplicantPageResponse(
        Boolean hasInterview,
        Integer currentPage,
        Integer totalPage,
        Integer totalCount,
        List<ApplicantSimpleResponse> applicants
) {
    public static ApplicantPageResponse from(ApplicantPageServiceResponse response) {
        return ApplicantPageResponse.builder()
                .hasInterview(response.hasInterview())
                .currentPage(response.currentPage())
                .totalPage(response.totalPage())
                .totalCount(response.totalCount())
                .applicants(response.applicants())
                .build();
    }
}

