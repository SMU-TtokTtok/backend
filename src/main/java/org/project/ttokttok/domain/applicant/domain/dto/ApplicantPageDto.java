package org.project.ttokttok.domain.applicant.domain.dto;

import lombok.Builder;

import java.util.List;

// 지원자 페이지 정보 DTO
@Builder
public record ApplicantPageDto(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ApplicantSimpleInfoDto> applicants
) {
}