package org.project.ttokttok.domain.notification.fcm.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.notification.fcm.domain.DeviceType;
import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenDeleteServiceRequest;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenSaveServiceRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FCMTokenService {

    private final FCMTokenRepository fcmTokenRepository;

    // FCM 토큰 저장
    public void save(FCMTokenSaveServiceRequest request) {
        DeviceType deviceType = DeviceType.fromString(
                request.deviceType()
        );

        FCMToken fcmToken = FCMToken.create(deviceType,
                request.email(),
                request.token()
        );

        fcmTokenRepository.save(fcmToken);
    }

    // FCM 토큰 삭제
    public void delete(FCMTokenDeleteServiceRequest request) {
        fcmTokenRepository.deleteByTokenAndEmail(
                request.token(),
                request.email()
        );
    }
}
