package org.project.ttokttok.domain.clubboard.repository;

import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClubBoardRepository extends JpaRepository<ClubBoard, String>, ClubBoardCustomRepository {

    /**
     * 특정 동아리의 게시글 단건을 club과 함께 조회합니다. (상세 조회용, N+1 방지 fetch join)
     */
    @Query("select b from ClubBoard b join fetch b.club where b.id = :boardId and b.club.id = :clubId")
    Optional<ClubBoard> findByIdAndClubIdWithClub(@Param("boardId") String boardId, @Param("clubId") String clubId);
}
