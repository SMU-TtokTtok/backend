package org.project.ttokttok.domain.notification.fcm.service;

import static org.project.ttokttok.global.exception.ErrorMessage.FCM_FIELD_BLANK;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.notification.fcm.domain.DeviceType;
import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenDeleteServiceRequest;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenSaveServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FCMTokenService {

    private final FCMTokenRepository fcmTokenRepository;

    // FCM 토큰 저장
    @Transactional
    public void saveOrUpdate(FCMTokenSaveServiceRequest request) {
        DeviceType deviceType = DeviceType.fromString(
                request.deviceType()
        );

        validateBlank(request.token());
        validateBlank(request.email());

        // 기존 토큰이 있는지 확인
        Optional<FCMToken> existingToken = fcmTokenRepository.findByEmailAndDeviceType(
                request.email(),
                deviceType
        );

        // 토큰이 있으면 업데이트, 없으면 저장.
        if (existingToken.isPresent()) {
            existingToken.get().updateToken(request.token());
        } else {
            FCMToken fcmToken = FCMToken.create(deviceType,
                    request.email(),
                    request.token()
            );
            fcmTokenRepository.save(fcmToken);
        }
    }

    // FCM 토큰 삭제
    public void delete(FCMTokenDeleteServiceRequest request) {
        fcmTokenRepository.deleteByTokenAndEmail(
                request.token(),
                request.email()
        );
    }

    private void validateBlank(String field) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException(FCM_FIELD_BLANK.getMessage());
        }
    }
}
