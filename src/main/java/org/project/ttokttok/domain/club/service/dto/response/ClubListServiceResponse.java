package org.project.ttokttok.domain.club.service.dto.response;

import java.util.List;

/**
 * 동아리 리스트 조회 서비스 응답 DTO
 * 서비스 레이어에서 동아리 목록 조회 결과를 담는 응답 DTO입니다.
 */
public record ClubListServiceResponse(
        List<ClubCardServiceResponse> clubs,
        int size,
        boolean hasNext,
        String nextCursor
) {
} 