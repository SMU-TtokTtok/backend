package org.project.ttokttok.domain.club.service.dto.response;

import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;

/**
 * 동아리 카드 정보 서비스 응답 DTO
 * 서비스 레이어에서 동아리 카드 정보를 담는 응답 DTO입니다.
 */
public record ClubCardServiceResponse(
        String id,                          // 동아리 ID
        String name,                        // 동아리 이름
        ClubType clubType,                  // 동아리 분류 (중앙/연합/학과)
        ClubCategory clubCategory,          // 동아리 카테고리 (봉사, 예술, 문화 등)
        String customCategory,              // 사용자 정의 카테고리
        String summary,                     // 동아리 한줄 소개
        String profileImageUrl,             // 동아리 프로필 이미지 URL
        int clubMemberCount,                // 현재 멤버 수
        boolean recruiting,                 // 모집 여부
        boolean bookmarked                  // 사용자 즐겨찾기 여부
) {
} 