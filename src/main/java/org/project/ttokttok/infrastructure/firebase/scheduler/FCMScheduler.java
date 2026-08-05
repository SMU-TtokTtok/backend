package org.project.ttokttok.infrastructure.firebase.scheduler;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FCMScheduler {

    private final FCMTokenRepository fcmTokenRepository;

    // 매일 새벽 4시에 실행 (초 분 시 일 월 요일)
    // 블루-그린 전환 창에서 blue/green 이 동시에 떠 있어도 한 인스턴스만 실행하도록 락을 건다.
    // lockAtLeastFor: 인스턴스 간 시계 오차로 락이 조기 해제돼 중복 실행되는 것을 막는다.
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(
            name = "FCMScheduler_cleanupStaleTokens",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT1M"
    )
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
