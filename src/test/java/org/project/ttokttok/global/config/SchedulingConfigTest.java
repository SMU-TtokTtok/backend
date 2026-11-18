package org.project.ttokttok.global.config;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;

/**
 * 메일 워커가 SMTP 대기로 스레드 하나를 오래 잡아도 다른 예약 작업(크론)이 돈다는 것을 실제 스케줄러 빈으로 확인한다(#465).
 *
 * <p>워커가 스레드를 최대 하나만 쓴다는 전제는 Spring의 규칙이다. {@code fixedDelay} 작업은 이전 실행이 끝난 뒤에야
 * 다음 실행을 예약하므로 자기 자신과 겹치지 않는다. 이 전제는 여기서 시험하지 않는다.
 * 크론끼리는 남은 스레드를 나눠 쓰므로, 같은 시각의 크론이 서로 기다리지 않는다는 보장은 아니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class SchedulingConfigTest {

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Test
    @DisplayName("스케줄러 풀 크기가 2로 설정된다")
    void schedulerPoolSizeIsTwo() {
        assertThat(taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize())
                .isEqualTo(SchedulingConfig.SCHEDULER_POOL_SIZE)
                .isEqualTo(2);
    }

    @Test
    @DisplayName("한 스레드가 막혀 있어도 다른 예약 작업은 실행된다 (풀이 1이면 실패)")
    void scheduledTaskRunsWhileAnotherThreadIsBlocked() throws Exception {
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        CountDownLatch otherTaskRan = new CountDownLatch(1);

        // SMTP 응답을 기다리며 스레드를 잡고 있는 메일 워커 역할
        ScheduledFuture<?> blocker = taskScheduler.schedule(() -> {
            blockerStarted.countDown();
            awaitUninterruptibly(releaseBlocker);
        }, Instant.now());
        try {
            assertThat(blockerStarted.await(5, SECONDS)).as("막는 작업이 시작됐다").isTrue();

            taskScheduler.schedule(otherTaskRan::countDown, Instant.now());

            assertThat(otherTaskRan.await(2, SECONDS))
                    .as("한 스레드가 막힌 동안 다른 예약 작업이 2초 안에 실행된다")
                    .isTrue();
        } finally {
            // 테스트 컨텍스트는 다른 테스트와 공유되므로 막아 둔 스레드를 반드시 풀어 준다.
            releaseBlocker.countDown();
            blocker.get(5, SECONDS);
        }
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
