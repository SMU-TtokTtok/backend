package org.project.ttokttok.infrastructure.firebase.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;

@ExtendWith(MockitoExtension.class)
class FCMSchedulerTest {

    @Mock
    private FCMTokenRepository fcmTokenRepository;

    @InjectMocks
    private FCMScheduler fcmScheduler;

    @Test
    @DisplayName("오래된 FCM 토큰 정리 작업이 정상적으로 실행된다")
    void cleanupStaleTokens() {
        // given
        int expectedDeletedCount = 5;
        given(fcmTokenRepository.deleteTokensOlderThan(any(LocalDateTime.class)))
                .willReturn(expectedDeletedCount);

        // when
        fcmScheduler.cleanupStaleTokens();

        // then
        // deleteTokensOlderThan 메서드가 한 번 호출되었는지 검증
        verify(fcmTokenRepository).deleteTokensOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("정리 작업에서 올바른 기준 날짜가 사용된다")
    void cleanupStaleTokens_CorrectCutoffDate() {
        // given
        given(fcmTokenRepository.deleteTokensOlderThan(any(LocalDateTime.class)))
                .willReturn(3);

        // when
        fcmScheduler.cleanupStaleTokens();

        // then
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(fcmTokenRepository).deleteTokensOlderThan(dateCaptor.capture());

        LocalDateTime capturedDate = dateCaptor.getValue();
        LocalDateTime expectedDate = LocalDateTime.now().minusMonths(2);

        // 2달 전 날짜가 맞는지 검증 (1분 정도의 오차 허용)
        assertThat(capturedDate).isBefore(LocalDateTime.now().minusMonths(2).plusMinutes(1));
        assertThat(capturedDate).isAfter(LocalDateTime.now().minusMonths(2).minusMinutes(1));
    }

    @Test
    @DisplayName("삭제할 토큰이 없어도 정상적으로 처리된다")
    void cleanupStaleTokens_NoTokensToDelete() {
        // given
        given(fcmTokenRepository.deleteTokensOlderThan(any(LocalDateTime.class)))
                .willReturn(0);

        // when
        fcmScheduler.cleanupStaleTokens();

        // then
        verify(fcmTokenRepository).deleteTokensOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("대량의 토큰 삭제도 정상적으로 처리된다")
    void cleanupStaleTokens_LargeNumber() {
        // given
        int largeDeleteCount = 10000;
        given(fcmTokenRepository.deleteTokensOlderThan(any(LocalDateTime.class)))
                .willReturn(largeDeleteCount);

        // when
        fcmScheduler.cleanupStaleTokens();

        // then
        verify(fcmTokenRepository).deleteTokensOlderThan(any(LocalDateTime.class));
    }
}
