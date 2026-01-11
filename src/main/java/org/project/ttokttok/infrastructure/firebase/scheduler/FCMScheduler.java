package org.project.ttokttok.infrastructure.firebase.scheduler;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FCMScheduler {

    private final FCMTokenRepository fcmTokenRepository;

    // 매일 새벽 4시에 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupStaleTokens() {
        // 1. 기준 시간 설정 (오늘로부터 2달 전)
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(2);

        log.info("오래된 FCM 토큰 정리 시작 - 기준 시점: {}", cutoffDate);

        // 2. 삭제 쿼리 실행
        int deletedCount = fcmTokenRepository.deleteTokensOlderThan(cutoffDate);

        log.info("오래된 FCM 토큰 정리 완료 - 삭제된 개수: {}개", deletedCount);
    }
}
