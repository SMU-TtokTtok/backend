package org.project.ttokttok.domain.notification.fcm.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FCMTokenTest {

    @Test
    @DisplayName("FCMToken을 생성할 수 있다")
    void createFCMToken() {
        // given
        DeviceType deviceType = DeviceType.ANDROID;
        String email = "test@example.com";
        String token = "sample-fcm-token";

        // when
        FCMToken fcmToken = FCMToken.create(deviceType, email, token);

        // then
        assertThat(fcmToken).isNotNull();
        assertThat(fcmToken.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(fcmToken.getEmail()).isEqualTo("test@example.com");
        assertThat(fcmToken.getToken()).isEqualTo("sample-fcm-token");
    }

    @Test
    @DisplayName("null DeviceType으로 FCMToken 생성 시 UNKNOWN으로 설정된다")
    void createFCMTokenWithNullDeviceType() {
        // given
        String email = "test@example.com";
        String token = "sample-fcm-token";

        // when
        FCMToken fcmToken = FCMToken.create(null, email, token);

        // then
        assertThat(fcmToken).isNotNull();
        assertThat(fcmToken.getDeviceType()).isEqualTo(DeviceType.UNKNOWN);
        assertThat(fcmToken.getEmail()).isEqualTo("test@example.com");
        assertThat(fcmToken.getToken()).isEqualTo("sample-fcm-token");
    }

    @Test
    @DisplayName("IOS DeviceType으로 FCMToken을 생성할 수 있다")
    void createIOSFCMToken() {
        // given
        DeviceType deviceType = DeviceType.IOS;
        String email = "ios@example.com";
        String token = "ios-fcm-token";

        // when
        FCMToken fcmToken = FCMToken.create(deviceType, email, token);

        // then
        assertThat(fcmToken).isNotNull();
        assertThat(fcmToken.getDeviceType()).isEqualTo(DeviceType.IOS);
        assertThat(fcmToken.getEmail()).isEqualTo("ios@example.com");
        assertThat(fcmToken.getToken()).isEqualTo("ios-fcm-token");
    }

    @Test
    @DisplayName("WEB DeviceType으로 FCMToken을 생성할 수 있다")
    void createWebFCMToken() {
        // given
        DeviceType deviceType = DeviceType.WEB;
        String email = "web@example.com";
        String token = "web-fcm-token";

        // when
        FCMToken fcmToken = FCMToken.create(deviceType, email, token);

        // then
        assertThat(fcmToken).isNotNull();
        assertThat(fcmToken.getDeviceType()).isEqualTo(DeviceType.WEB);
        assertThat(fcmToken.getEmail()).isEqualTo("web@example.com");
        assertThat(fcmToken.getToken()).isEqualTo("web-fcm-token");
    }

    @Test
    @DisplayName("UNKNOWN DeviceType으로 FCMToken을 생성할 수 있다")
    void createUnknownFCMToken() {
        // given
        DeviceType deviceType = DeviceType.UNKNOWN;
        String email = "unknown@example.com";
        String token = "unknown-fcm-token";

        // when
        FCMToken fcmToken = FCMToken.create(deviceType, email, token);

        // then
        assertThat(fcmToken).isNotNull();
        assertThat(fcmToken.getDeviceType()).isEqualTo(DeviceType.UNKNOWN);
        assertThat(fcmToken.getEmail()).isEqualTo("unknown@example.com");
        assertThat(fcmToken.getToken()).isEqualTo("unknown-fcm-token");
    }

    @Test
    @DisplayName("빈 이메일로 FCMToken을 생성하면 예외를 발생시킨다.")
    void createFCMTokenWithEmptyEmail() {
        // given
        DeviceType deviceType = DeviceType.ANDROID;
        String email = "";
        String token = "sample-fcm-token";

        // when & then
       assertThatThrownBy(() ->  FCMToken.create(deviceType, email, token))
               .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("빈 토큰으로 FCMToken을 생성하면 예외를 발생시킨다.")
    void createFCMTokenWithEmptyToken() {
        // given
        DeviceType deviceType = DeviceType.IOS;
        String email = "test@example.com";
        String token = "";

        // then
        assertThatThrownBy(() ->  FCMToken.create(deviceType, email, token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 이메일로 FCMToken을 생성하면 예외를 발생시킨다.")
    void createFCMTokenWithNullEmail() {
        // given
        DeviceType deviceType = DeviceType.WEB;
        String token = "sample-fcm-token";

        // when & then
        assertThatThrownBy(() ->  FCMToken.create(deviceType, null, token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 토큰으로 FCMToken을 생성하면 예외를 발생시킨다.")
    void createFCMTokenWithNullToken() {
        // given
        DeviceType deviceType = DeviceType.ANDROID;
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() ->  FCMToken.create(deviceType, email, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
