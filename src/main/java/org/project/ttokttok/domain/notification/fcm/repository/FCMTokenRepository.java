package org.project.ttokttok.domain.notification.fcm.repository;

import java.util.List;
import java.util.Optional;
import org.project.ttokttok.domain.notification.fcm.domain.DeviceType;
import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FCMTokenRepository extends JpaRepository<FCMToken, String> {
    void deleteByTokenAndEmail(String token, String email);

    List<FCMToken> findByEmail(String email);

    Optional<FCMToken> findByEmailAndDeviceType(String email, DeviceType deviceType);

    boolean existsByEmailAndDeviceType(String email, DeviceType deviceType);
}
