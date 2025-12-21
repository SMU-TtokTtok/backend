package org.project.ttokttok.domain.notification.fcm.domain;

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
    public static FCMToken create(String id, DeviceType deviceType, String email, String token) {
        return FCMToken.builder()
                .id(id)
                .deviceType(deviceType != null ? deviceType : DeviceType.UNKNOWN)
                .email(email)
                .token(token)
                .build();
    }
}
