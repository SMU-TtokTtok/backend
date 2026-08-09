package org.project.ttokttok.domain.clubMember.repository.dto;

/**
 * 동아리별 멤버 수 집계 결과
 *
 * <p>{@code Club.clubMembers} 는 지연 로딩 컬렉션이라 {@code size()} 만 호출해도 멤버 행 전체가
 * 적재된다. 카드 목록처럼 개수만 필요한 곳에서는 이 집계 결과를 사용한다.
 */
public record ClubMemberCountQueryDto(
        String clubId,
        long count
) {
}
