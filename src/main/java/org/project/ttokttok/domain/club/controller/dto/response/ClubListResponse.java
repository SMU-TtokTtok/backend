package org.project.ttokttok.domain.club.controller.dto.response;

import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;

import java.util.List;

/**
 * 동아리 리스트 조회 API 응답 DTO
 * 메인 화면에서 동아리 목록을 표시할 때 사용됩니다.
 * */
public record ClubListResponse(
        List<ClubCardResponse> clubs,        // 동아리 카드 리스트
        int size,                           // 현재 로드된 개수
        boolean hasNext,                    // 다음 데이터 존재 여부
        String nextCursor                   // 다음 요청시 사용할 커서 (null이면 마지막)
) {
    public static ClubListResponse from(ClubListServiceResponse response) {
        return new ClubListResponse(
                response.clubs().stream()
                        .map(ClubCardResponse::from)
                        .toList(),
                response.size(),
                response.hasNext(),
                response.nextCursor()
        );
    }
} 