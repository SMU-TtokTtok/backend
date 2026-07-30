package org.project.ttokttok.domain.club.repository;

import org.project.ttokttok.domain.club.domain.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, String>, ClubCustomRepository{
    @Query("SELECT c from Club c where c.admin.username = :username")
    Optional<Club> findByAdminUsername(String username);

    Optional<Club> findByName(String clubName);

    // 조회수 원자적 증가 (동시 상세 조회 시 read-modify-write 경쟁으로 인한 조회수 유실 방지)
    @Modifying
    @Query("UPDATE Club c SET c.viewCount = c.viewCount + 1 WHERE c.id = :clubId")
    int increaseViewCount(@Param("clubId") String clubId);
}
