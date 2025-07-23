package org.project.ttokttok.domain.clubMember.repository;

import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberInExcelResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember, String>, ClubMemberCustomRepository {
    @Query("SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.role = :role")
    Optional<ClubMember> findByClubIdAndRole(String clubId, MemberRole role);

    @Query("SELECT new org.project.ttokttok.domain.clubMember.service.dto.response." +
            "ClubMemberInExcelResponse(cm.grade, cm.user.name, cm.major, cm.role) " +
           "FROM ClubMember cm WHERE cm.club.id = :clubId")
    List<ClubMemberInExcelResponse> findByClubId(String clubId);

    @Query("SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.name LIKE %:keyword%")
    List<ClubMember> findByClubIdAndKeyword(String clubId, String keyword);
}
