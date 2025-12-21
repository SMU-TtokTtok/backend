package org.project.ttokttok.domain.notification.fcm.domain;

public enum DeviceType {
    WEB,
    ANDROID,
    IOS,
    UNKNOWN;

    public static DeviceType fromString(String deviceType) {
        try {
            return DeviceType.valueOf(
                    deviceType.toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
