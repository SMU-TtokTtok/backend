package org.project.ttokttok.domain.clubMember.repository;

import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember, String>, ClubMemberCustomRepository {
    @Query("SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.role = :role")
    Optional<ClubMember> findByClubIdAndRole(String clubId, MemberRole role);
}
