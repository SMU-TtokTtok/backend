package org.project.ttokttok.domain.applicant.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.dto.ApplicantSimpleInfoDto;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantPageServiceResponse;

import java.util.List;

@Builder
public record ApplicantPageResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ApplicantSimpleInfoDto> applicants
) {
    public static ApplicantPageResponse from(ApplicantPageServiceResponse serviceResponse) {
        return ApplicantPageResponse.builder()
                .currentPage(serviceResponse.currentPage())
                .totalPage(serviceResponse.totalPage())
                .totalCount(serviceResponse.totalCount())
                .applicants(serviceResponse.applicants())
                .build();
    }
}

