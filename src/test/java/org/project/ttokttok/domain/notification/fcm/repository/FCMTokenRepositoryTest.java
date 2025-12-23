package org.project.ttokttok.domain.notification.fcm.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.notification.fcm.domain.DeviceType;
import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Transactional
class FCMTokenRepositoryTest {

    @Autowired
    private FCMTokenRepository fcmTokenRepository;

    @Test
    @DisplayName("FCM 토큰을 저장할 수 있다")
    void saveToken() {
        // given
        FCMToken fcmToken = FCMToken.create(
                DeviceType.ANDROID,
                "test@example.com",
                "sample-fcm-token"
        );

        // when
        FCMToken savedToken = fcmTokenRepository.save(fcmToken);

        // then
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(savedToken.getEmail()).isEqualTo("test@example.com");
        assertThat(savedToken.getToken()).isEqualTo("sample-fcm-token");
    }

    @Test
    @DisplayName("저장된 FCM 토큰을 조회할 수 있다")
    void findToken() {
        // given
        FCMToken fcmToken = FCMToken.create(
                DeviceType.IOS,
                "user@example.com",
                "ios-fcm-token"
        );
        FCMToken savedToken = fcmTokenRepository.save(fcmToken);

        // when
        Optional<FCMToken> foundToken = fcmTokenRepository.findById(savedToken.getId());

        // then
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getDeviceType()).isEqualTo(DeviceType.IOS);
        assertThat(foundToken.get().getEmail()).isEqualTo("user@example.com");
        assertThat(foundToken.get().getToken()).isEqualTo("ios-fcm-token");
    }

    @Test
    @DisplayName("모든 FCM 토큰을 조회할 수 있다")
    void findAllTokens() {
        // given
        FCMToken token1 = FCMToken.create(DeviceType.WEB, "user1@example.com", "web-token-1");
        FCMToken token2 = FCMToken.create(DeviceType.ANDROID, "user2@example.com", "android-token-1");
        FCMToken token3 = FCMToken.create(DeviceType.IOS, "user3@example.com", "ios-token-1");

        fcmTokenRepository.save(token1);
        fcmTokenRepository.save(token2);
        fcmTokenRepository.save(token3);

        // when
        List<FCMToken> allTokens = fcmTokenRepository.findAll();

        // then
        assertThat(allTokens).hasSize(3);
        assertThat(allTokens)
                .extracting(FCMToken::getEmail)
                .containsExactlyInAnyOrder("user1@example.com", "user2@example.com", "user3@example.com");
    }

    @Test
    @DisplayName("토큰과 이메일로 FCM 토큰을 삭제할 수 있다")
    void deleteByTokenAndEmail() {
        // given
        FCMToken fcmToken = FCMToken.create(
                DeviceType.ANDROID,
                "delete@example.com",
                "token-to-delete"
        );
        fcmTokenRepository.save(fcmToken);

        // when
        fcmTokenRepository.deleteByTokenAndEmail("token-to-delete", "delete@example.com");

        // then
        List<FCMToken> remainingTokens = fcmTokenRepository.findAll();
        assertThat(remainingTokens).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 토큰과 이메일로 삭제 시도해도 예외가 발생하지 않는다")
    void deleteNonExistentToken() {
        // given
        FCMToken fcmToken = FCMToken.create(
                DeviceType.WEB,
                "existing@example.com",
                "existing-token"
        );
        fcmTokenRepository.save(fcmToken);

        // when & then
        assertThatCode(() -> {
            fcmTokenRepository.deleteByTokenAndEmail("non-existent-token", "non-existent@example.com");
        }).doesNotThrowAnyException();

        // 기존 토큰은 여전히 존재해야 함
        List<FCMToken> allTokens = fcmTokenRepository.findAll();
        assertThat(allTokens).hasSize(1);
    }

    @Test
    @DisplayName("같은 토큰이지만 다른 이메일로 삭제 시도 시 삭제되지 않는다")
    void deleteWithWrongEmail() {
        // given
        FCMToken fcmToken = FCMToken.create(
                DeviceType.ANDROID,
                "correct@example.com",
                "shared-token"
        );
        fcmTokenRepository.save(fcmToken);

        // when
        fcmTokenRepository.deleteByTokenAndEmail("shared-token", "wrong@example.com");

        // then
        List<FCMToken> remainingTokens = fcmTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getEmail()).isEqualTo("correct@example.com");
    }

    @Test
    @DisplayName("같은 이메일이지만 다른 토큰으로 삭제 시도 시 삭제되지 않는다")
    void deleteWithWrongToken() {
        // given
        FCMToken fcmToken = FCMToken.create(
                DeviceType.IOS,
                "user@example.com",
                "correct-token"
        );
        fcmTokenRepository.save(fcmToken);

        // when
        fcmTokenRepository.deleteByTokenAndEmail("wrong-token", "user@example.com");

        // then
        List<FCMToken> remainingTokens = fcmTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getToken()).isEqualTo("correct-token");
    }

    @Test
    @DisplayName("여러 토큰 중에서 특정 토큰과 이메일 조합만 삭제된다")
    void deleteSpecificTokenAmongMultiple() {
        // given
        FCMToken token1 = FCMToken.create(DeviceType.WEB, "user1@example.com", "token1");
        FCMToken token2 = FCMToken.create(DeviceType.ANDROID, "user2@example.com", "token2");
        FCMToken token3 = FCMToken.create(DeviceType.IOS, "user3@example.com", "token3");

        fcmTokenRepository.save(token1);
        fcmTokenRepository.save(token2);
        fcmTokenRepository.save(token3);

        // when
        fcmTokenRepository.deleteByTokenAndEmail("token2", "user2@example.com");

        // then
        List<FCMToken> remainingTokens = fcmTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(2);
        assertThat(remainingTokens)
                .extracting(FCMToken::getEmail)
                .containsExactlyInAnyOrder("user1@example.com", "user3@example.com");
    }

    @Test
    @DisplayName("같은 이메일과 디바이스 타입으로 두 번째 토큰을 저장하려고 하면 제약조건 위반 예외가 발생한다")
    void saveDuplicateEmailAndDeviceType() {
        // given
        String email = "test@example.com";
        DeviceType deviceType = DeviceType.ANDROID;

        FCMToken firstToken = FCMToken.create(deviceType, email, "token1");
        fcmTokenRepository.save(firstToken);

        // when & then
        FCMToken secondToken = FCMToken.create(deviceType, email, "token2");

        assertThatThrownBy(() -> fcmTokenRepository.saveAndFlush(secondToken))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 이메일이지만 다른 디바이스 타입으로는 토큰을 저장할 수 있다")
    void saveSameEmailDifferentDevice() {
        // given
        String email = "uniquetest@example.com";

        FCMToken androidToken = FCMToken.create(DeviceType.ANDROID, email, "android_token");
        FCMToken iosToken = FCMToken.create(DeviceType.IOS, email, "ios_token");

        // when
        fcmTokenRepository.save(androidToken);
        fcmTokenRepository.save(iosToken);

        // then
        assertThat(fcmTokenRepository.findByEmail(email)).hasSize(2);
    }

    @Test
    @DisplayName("다른 이메일이지만 같은 디바이스 타입으로는 토큰을 저장할 수 있다")
    void saveDifferentEmailSameDevice() {
        // given
        DeviceType deviceType = DeviceType.ANDROID;

        FCMToken token1 = FCMToken.create(deviceType, "uniqueuser1@example.com", "token1");
        FCMToken token2 = FCMToken.create(deviceType, "uniqueuser2@example.com", "token2");

        // when
        fcmTokenRepository.save(token1);
        fcmTokenRepository.save(token2);

        // then
        assertThat(fcmTokenRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("이메일과 디바이스 타입으로 토큰을 조회할 수 있다")
    void findByEmailAndDeviceType() {
        // given
        String email = "findtest@example.com";
        DeviceType deviceType = DeviceType.WEB;
        String tokenValue = "web_token";

        FCMToken fcmToken = FCMToken.create(deviceType, email, tokenValue);
        fcmTokenRepository.save(fcmToken);

        // when
        Optional<FCMToken> foundToken = fcmTokenRepository.findByEmailAndDeviceType(email, deviceType);

        // then
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getToken()).isEqualTo(tokenValue);
        assertThat(foundToken.get().getEmail()).isEqualTo(email);
        assertThat(foundToken.get().getDeviceType()).isEqualTo(deviceType);
    }

    @Test
    @DisplayName("존재하지 않는 이메일과 디바이스 타입으로 조회하면 빈 Optional을 반환한다")
    void findByNonExistentEmailAndDevice() {
        // when
        Optional<FCMToken> foundToken = fcmTokenRepository.findByEmailAndDeviceType("nonexistent@example.com", DeviceType.ANDROID);

        // then
        assertThat(foundToken).isEmpty();
    }
}
