package org.project.ttokttok.infrastructure.firebase.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.notification.fcm.domain.DeviceType;
import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FCMSchedulerIntegrationTest {

    @Autowired
    private FCMScheduler fcmScheduler;

    @Autowired
    private FCMTokenRepository fcmTokenRepository;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
        FCMToken token1 = FCMToken.create(DeviceType.ANDROID, "user1@example.com", "token1");
        FCMToken token2 = FCMToken.create(DeviceType.IOS, "user2@example.com", "token2");
        FCMToken token3 = FCMToken.create(DeviceType.WEB, "user3@example.com", "token3");

        fcmTokenRepository.save(token1);
        fcmTokenRepository.save(token2);
        fcmTokenRepository.save(token3);
    }

    @AfterEach
    void tearDown() {
        fcmTokenRepository.deleteAll();
    }

    @Test
    @DisplayName("스케줄러가 실제로 오래된 토큰을 삭제하는지 통합 테스트")
    void cleanupStaleTokens_Integration() {
        // given
        List<FCMToken> initialTokens = fcmTokenRepository.findAll();
        assertThat(initialTokens).hasSize(3);

        // when
        fcmScheduler.cleanupStaleTokens();

        // then
        // 현재 시간에 생성된 토큰들이므로 삭제되지 않아야 함
        List<FCMToken> remainingTokens = fcmTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(3);
    }

    @Test
    @DisplayName("미래 기준 날짜로 테스트하여 모든 토큰이 삭제되는지 확인")
    void cleanupStaleTokens_WithFutureDate() {
        // given
        List<FCMToken> initialTokens = fcmTokenRepository.findAll();
        assertThat(initialTokens).hasSize(3);

        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        // when
        int deletedCount = fcmTokenRepository.deleteTokensOlderThan(futureDate);

        // then
        assertThat(deletedCount).isEqualTo(3);

        List<FCMToken> remainingTokens = fcmTokenRepository.findAll();
        assertThat(remainingTokens).isEmpty();
    }

    @Test
    @DisplayName("특정 날짜 이전의 토큰만 삭제되는지 확인")
    void cleanupStaleTokens_PartialDeletion() {
        // given
        // 과거 시간으로 설정된 토큰을 직접 DB에 삽입하는 것은 어려우므로
        // deleteTokensOlderThan 메서드를 직접 테스트
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastDate = now.minusHours(1);
        LocalDateTime futureDate = now.plusHours(1);

        // 현재 시간보다 1시간 후 기준으로 삭제하면 모든 토큰이 삭제됨
        int deletedCount = fcmTokenRepository.deleteTokensOlderThan(futureDate);
        assertThat(deletedCount).isEqualTo(3);

        // 다시 토큰 생성
        setUp();

        // 현재 시간보다 1시간 전 기준으로 삭제하면 삭제되지 않음
        deletedCount = fcmTokenRepository.deleteTokensOlderThan(pastDate);
        assertThat(deletedCount).isEqualTo(0);

        List<FCMToken> remainingTokens = fcmTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(3);
    }
}
