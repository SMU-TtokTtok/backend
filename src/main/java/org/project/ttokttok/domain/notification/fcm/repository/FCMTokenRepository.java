package org.project.ttokttok.domain.notification.fcm.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.project.ttokttok.domain.notification.fcm.domain.DeviceType;
import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FCMTokenRepository extends JpaRepository<FCMToken, String> {
    void deleteByTokenAndEmail(String token, String email);

    List<FCMToken> findByEmail(String email);

    Optional<FCMToken> findByEmailAndDeviceType(String email, DeviceType deviceType);

    // 동아리를 즐겨찾기한 사용자의 FCM 토큰 목록을 받아옴
    @Query("SELECT f.token FROM FCMToken f INNER JOIN Favorite fa "
            + "ON fa.user.email = f.email "
            + "WHERE fa.club.id = :clubId")
    List<String> findTokensByClubId(@Param("clubId") String clubId);

    // 입력받은 날짜보다 이전에 갱신된 토큰들을 전부 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FCMToken f WHERE f.updatedAt < :cutoffDate")
    int deleteTokensOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FCMToken f WHERE f.token IN :tokens")
    int deleteFCMTokenByTokens(@Param("tokens") List<String> tokens);
}
