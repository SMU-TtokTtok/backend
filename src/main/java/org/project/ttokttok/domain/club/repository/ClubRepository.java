package org.project.ttokttok.domain.club.repository;

import org.project.ttokttok.domain.club.domain.Club;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, String>, ClubCustomRepository{
    @Query("SELECT c from Club c where c.admin.username = :username")
    Optional<Club> findByAdminUsername(String username);

    // 같은 동아리의 부원 등록(합격 확정, 수동 등록)을 직렬화한다. 조회 후 저장 사이의 경쟁으로
    // 같은 (동아리, 이메일) 부원이 중복 저장되는 것을 막는다. 트랜잭션 안에서만 사용한다. (#432)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Club c WHERE c.id = :clubId")
    Optional<Club> findByIdForUpdate(@Param("clubId") String clubId);

    Optional<Club> findByName(String clubName);

    // 조회수 원자적 증가 (동시 상세 조회 시 read-modify-write 경쟁으로 인한 조회수 유실 방지)
    @Modifying
    @Query("UPDATE Club c SET c.viewCount = c.viewCount + 1 WHERE c.id = :clubId")
    int increaseViewCount(@Param("clubId") String clubId);
}
