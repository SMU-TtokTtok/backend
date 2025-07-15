package org.project.ttokttok.domain.applicant.service.dto.response;

import org.project.ttokttok.domain.applicant.domain.dto.ApplicantPageDto;
import org.project.ttokttok.domain.applicant.domain.dto.ApplicantSimpleInfoDto;

import java.util.List;

public record ApplicantPageServiceResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ApplicantSimpleInfoDto> applicants
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
