package org.project.ttokttok.domain.applicant.repository.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.dto.ApplicantPageDto;
import org.project.ttokttok.domain.applicant.domain.dto.ApplicantSimpleInfoDto;

import java.util.List;

@Builder
public record ApplicantPageQueryResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ApplicantSimpleInfoDto> applicants
) {
    // 도메인 DTO로 변환하는 메서드 추가
    public ApplicantPageDto toDto() {
        return ApplicantPageDto.builder()
                .currentPage(currentPage)
                .totalPage(totalPage)
                .totalCount(totalCount)
                .applicants(applicants.stream()
                        .map(ApplicantSimpleInfoDto::toResponse)
                        .toList())
                .build();
    }
}