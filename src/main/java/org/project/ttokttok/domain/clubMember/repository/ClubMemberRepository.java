package org.project.ttokttok.domain.clubMember.repository;

import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberCountQueryDto;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberInExcelResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember, String>, ClubMemberCustomRepository {
    @Query("SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.role = :role")
    Optional<ClubMember> findByClubIdAndRole(String clubId, MemberRole role);

    @Query("SELECT new org.project.ttokttok.domain.clubMember.service.dto.response." +
            "ClubMemberInExcelResponse(cm.grade, cm.memberName, cm.major, cm.role) " +
           "FROM ClubMember cm WHERE cm.club.id = :clubId")
    List<ClubMemberInExcelResponse> findByClubId(String clubId);

    @Query("SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.memberName LIKE %:keyword%")
    List<ClubMember> findByClubIdAndKeyword(String clubId, String keyword);

    boolean existsByClubIdAndEmail(String clubId, String email);

    /**
     * 여러 동아리의 멤버 수를 한 번에 집계
     *
     * <p>멤버가 없는 동아리는 결과에 포함되지 않으므로, 호출부에서 0 으로 기본값 처리해야 한다.
     */
    @Query("SELECT new org.project.ttokttok.domain.clubMember.repository.dto." +
            "ClubMemberCountQueryDto(cm.club.id, COUNT(cm)) " +
           "FROM ClubMember cm WHERE cm.club.id IN :clubIds GROUP BY cm.club.id")
    List<ClubMemberCountQueryDto> countClubMembersForEach(@Param("clubIds") List<String> clubIds);
}
