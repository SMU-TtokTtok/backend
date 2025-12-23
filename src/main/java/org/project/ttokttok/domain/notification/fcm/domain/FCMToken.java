package org.project.ttokttok.domain.notification.fcm.domain;

import static org.project.ttokttok.global.exception.ErrorMessage.FCM_EMAIL_BLANK;
import static org.project.ttokttok.global.exception.ErrorMessage.FCM_TOKEN_BLANK;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.global.entity.BaseTimeEntity;
import org.project.ttokttok.global.exception.ErrorMessage;

@Entity
@Getter
@Table(name = "fcm_token")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FCMToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(nullable = false, updatable = false)
    private String email;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Builder
    private FCMToken(DeviceType deviceType, String email, String token) {
        this.deviceType = deviceType;
        this.email = email;
        this.token = token;
    }

    public static FCMToken create(DeviceType deviceType, String email, String token) {
        validateEmail(email);
        validateToken(token);

        return FCMToken.builder()
                .deviceType(deviceType != null ? deviceType : DeviceType.UNKNOWN)
                .email(email)
                .token(token)
                .build();
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(FCM_EMAIL_BLANK.getMessage());
        }
    }

    private static void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(FCM_TOKEN_BLANK.getMessage());
        }
    }
}
