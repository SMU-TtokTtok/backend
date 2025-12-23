package org.project.ttokttok.domain.favorite.repository;

import java.util.List;
import java.util.Optional;
import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, String>, FavoriteCustomRepository {
    
    /**
     * 사용자와 동아리 조합으로 즐겨찾기 조회
     */
    @Query("SELECT f FROM Favorite f WHERE f.user.email = :userEmail AND f.club.id = :clubId")
    Optional<Favorite> findByUserEmailAndClubId(@Param("userEmail") String userEmail, 
                                               @Param("clubId") String clubId);
    
    /**
     * 사용자의 모든 즐겨찾기 조회 (동아리 정보 포함)
     * @deprecated 페이징 기능이 추가된 findFavoritesByUserEmail(userEmail, pageable) 사용을 권장합니다
     */
    @Deprecated
    @Query("SELECT f FROM Favorite f " +
           "JOIN FETCH f.club c " +
           "JOIN FETCH c.admin " +
           "WHERE f.user.email = :userEmail " +
           "ORDER BY f.createdAt DESC")
    List<Favorite> findAllByUserEmailWithClub(@Param("userEmail") String userEmail);
    
    /**
     * 사용자와 동아리 조합으로 즐겨찾기 존재 여부 확인
     */
    @Query("SELECT COUNT(f) > 0 FROM Favorite f WHERE f.user.email = :userEmail AND f.club.id = :clubId")
    boolean existsByUserEmailAndClubId(@Param("userEmail") String userEmail, 
                                      @Param("clubId") String clubId);
    
    /**
     * 동아리의 즐겨찾기 수 조회
     */
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.club.id = :clubId")
    long countByClubId(@Param("clubId") String clubId);
}
