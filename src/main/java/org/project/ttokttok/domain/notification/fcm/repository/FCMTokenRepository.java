package org.project.ttokttok.domain.notification.fcm.repository;

import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FCMTokenRepository extends JpaRepository<FCMToken, String> {
    void deleteByTokenAndEmail(String token, String email);
}
