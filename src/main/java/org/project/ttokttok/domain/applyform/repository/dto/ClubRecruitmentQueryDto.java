package org.project.ttokttok.domain.applyform.repository.dto;

import java.time.LocalDate;

/**
 * 동아리별 모집중 지원폼 조회 결과
 *
 * <p>모집중({@code status = ACTIVE} 이면서 {@code isRecruiting = true}) 인 지원폼만 담는다.
 * 따라서 이 DTO 가 존재한다는 사실 자체가 해당 동아리가 모집중이라는 의미다.
 *
 * <p>지원폼 엔티티 대신 이 projection 을 쓰는 이유는 {@code ApplyForm.club} 이 지연 로딩이라,
 * 엔티티를 받아 {@code getClub().getId()} 로 묶으면 프록시 초기화가 일어나 N+1 이 되살아나기 때문이다.
 * JPQL 의 {@code a.club.id} 는 FK 컬럼을 그대로 읽으므로 추가 조회가 없다.
 */
public record ClubRecruitmentQueryDto(
        String clubId,
        LocalDate applyEndDate
) {
}
