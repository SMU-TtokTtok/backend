package org.project.ttokttok.domain.notification.fcm.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.notification.fcm.domain.DeviceType;
import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenDeleteServiceRequest;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenSaveServiceRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FCMTokenServiceTest {

    @Mock
    private FCMTokenRepository fcmTokenRepository;

    @InjectMocks
    private FCMTokenService fcmTokenService;

    @Test
    @DisplayName("FCM 토큰을 저장할 수 있다")
    void saveToken() {
        // given
        FCMTokenSaveServiceRequest request = createSaveRequest("test@example.com", "sample-fcm-token", "ANDROID");

        // when
        fcmTokenService.save(request);

        // then
        verify(fcmTokenRepository, times(1)).save(any(FCMToken.class));
    }

    @Test
    @DisplayName("FCM 토큰 저장 시 DeviceType이 올바르게 변환된다")
    void saveTokenWithDeviceTypeConversion() {
        // given
        FCMTokenSaveServiceRequest request = createSaveRequest("test@example.com", "sample-fcm-token", "ios");

        // when
        fcmTokenService.save(request);

        // then
        verifySaveWithToken(DeviceType.IOS, "test@example.com", "sample-fcm-token");
    }

    @Test
    @DisplayName("FCM 토큰 저장 시 잘못된 DeviceType은 UNKNOWN으로 변환된다")
    void saveTokenWithInvalidDeviceType() {
        // given
        FCMTokenSaveServiceRequest request = createSaveRequest("test@example.com", "sample-fcm-token", "INVALID_TYPE");

        // when
        fcmTokenService.save(request);

        // then
        verifySaveWithToken(DeviceType.UNKNOWN, "test@example.com", "sample-fcm-token");
    }

    @Test
    @DisplayName("FCM 토큰 저장 시 null DeviceType은 UNKNOWN으로 변환된다")
    void saveTokenWithNullDeviceType() {
        // given
        FCMTokenSaveServiceRequest request = createSaveRequest("test@example.com", "sample-fcm-token", null);

        // when
        fcmTokenService.save(request);

        // then
        verifySaveWithToken(DeviceType.UNKNOWN, "test@example.com", "sample-fcm-token");
    }

    @Test
    @DisplayName("WEB 디바이스 타입으로 FCM 토큰을 저장할 수 있다")
    void saveWebToken() {
        // given
        FCMTokenSaveServiceRequest request = createSaveRequest("web@example.com", "web-fcm-token", "WEB");

        // when
        fcmTokenService.save(request);

        // then
        verifySaveWithToken(DeviceType.WEB, "web@example.com", "web-fcm-token");
    }

    @Test
    @DisplayName("FCM 토큰을 삭제할 수 있다")
    void deleteToken() {
        // given
        FCMTokenDeleteServiceRequest request = createDeleteRequest("delete@example.com", "token-to-delete");

        // when
        fcmTokenService.delete(request);

        // then
        verify(fcmTokenRepository, times(1)).deleteByTokenAndEmail("token-to-delete", "delete@example.com");
    }

    @Test
    @DisplayName("여러 개의 FCM 토큰을 순차적으로 저장할 수 있다")
    void saveMultipleTokens() {
        // given
        FCMTokenSaveServiceRequest request1 = createSaveRequest("user1@example.com", "token1", "ANDROID");
        FCMTokenSaveServiceRequest request2 = createSaveRequest("user2@example.com", "token2", "IOS");

        // when
        fcmTokenService.save(request1);
        fcmTokenService.save(request2);

        // then
        verify(fcmTokenRepository, times(2)).save(any(FCMToken.class));
        verifySaveWithEmailAndDevice("user1@example.com", DeviceType.ANDROID);
        verifySaveWithEmailAndDevice("user2@example.com", DeviceType.IOS);
    }

    @Test
    @DisplayName("여러 개의 FCM 토큰을 순차적으로 삭제할 수 있다")
    void deleteMultipleTokens() {
        // given
        FCMTokenDeleteServiceRequest request1 = createDeleteRequest("user1@example.com", "token1");
        FCMTokenDeleteServiceRequest request2 = createDeleteRequest("user2@example.com", "token2");

        // when
        fcmTokenService.delete(request1);
        fcmTokenService.delete(request2);

        // then
        verify(fcmTokenRepository, times(2)).deleteByTokenAndEmail(anyString(), anyString());
        verify(fcmTokenRepository).deleteByTokenAndEmail("token1", "user1@example.com");
        verify(fcmTokenRepository).deleteByTokenAndEmail("token2", "user2@example.com");
    }

    @Test
    @DisplayName("빈 문자열 이메일로 저장 시도 시, 예외가 발생한다.")
    void saveTokenWithEmptyEmail() {
        // given
        FCMTokenSaveServiceRequest request = createSaveRequest("", "sample-token", "ANDROID");

        // when & then
        assertThatThrownBy(() -> fcmTokenService.save(request))
                .isInstanceOf(IllegalArgumentException.class);


        // verify
        verify(fcmTokenRepository, never()).save(any(FCMToken.class));
    }

    @Test
    @DisplayName("빈 문자열 토큰으로 저장 시도 시, 예외가 발생한다.")
    void saveTokenWithEmptyToken() {
        // given
        FCMTokenSaveServiceRequest request = createSaveRequest("test@example.com", "", "IOS");

        // when & then
        assertThatThrownBy(() -> fcmTokenService.save(request))
                .isInstanceOf(IllegalArgumentException.class);


        // verify
        verify(fcmTokenRepository, never()).save(any(FCMToken.class));
    }

    @Test
    @DisplayName("대소문자 혼합 DeviceType도 올바르게 처리된다")
    void saveTokenWithMixedCaseDeviceType() {
        // given
        FCMTokenSaveServiceRequest request = createSaveRequest("test@example.com", "sample-token", "AnDrOiD");

        // when
        fcmTokenService.save(request);

        // then
        verifySaveWithToken(DeviceType.ANDROID, "test@example.com", "sample-token");
    }

    /* 헬퍼 메서드 */
    private FCMTokenSaveServiceRequest createSaveRequest(String email, String token, String deviceType) {
        return FCMTokenSaveServiceRequest.builder()
                .email(email)
                .token(token)
                .deviceType(deviceType)
                .build();
    }

    private FCMTokenDeleteServiceRequest createDeleteRequest(String email, String token) {
        return FCMTokenDeleteServiceRequest.builder()
                .email(email)
                .token(token)
                .build();
    }

    private void verifySaveWithToken(DeviceType expectedDeviceType, String expectedEmail, String expectedToken) {
        verify(fcmTokenRepository, times(1)).save(argThat(fcmToken ->
                fcmToken.getDeviceType() == expectedDeviceType &&
                fcmToken.getEmail().equals(expectedEmail) &&
                fcmToken.getToken().equals(expectedToken)
        ));
    }

    private void verifySaveWithEmailAndDevice(String expectedEmail, DeviceType expectedDeviceType) {
        verify(fcmTokenRepository).save(argThat(fcmToken ->
                fcmToken.getEmail().equals(expectedEmail) &&
                fcmToken.getDeviceType() == expectedDeviceType
        ));
    }
}
