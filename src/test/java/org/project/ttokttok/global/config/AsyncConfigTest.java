package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@DisplayName("AsyncConfig")
class AsyncConfigTest {

    @Test
    @DisplayName("fcmExecutor는 스레드 2개, 큐 100, prefix=\"fcm-\"의 유계 스레드풀이다 (#435)")
    void fcmExecutorIsBoundedPool() {
        // given
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) new AsyncConfig().fcmExecutor();

        // then
        assertThat(pool.getCorePoolSize()).isEqualTo(2);
        assertThat(pool.getMaxPoolSize()).isEqualTo(2);
        assertThat(pool.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(100);
        assertThat(pool.getThreadNamePrefix()).isEqualTo("fcm-");
        pool.shutdown();
    }

    @Test
    @Timeout(10)
    @DisplayName("fcmExecutor가 가득 차면 넘친 알림을 호출 스레드에서 실행하지 않고 버린다 (#435)")
    void fcmExecutorDiscardsOverflowWithoutRunningOnCaller() throws Exception {
        // given: 스레드 2개를 붙잡고 큐 100칸을 채운다
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) new AsyncConfig().fcmExecutor();
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch running = new CountDownLatch(2);
        try {
            for (int i = 0; i < 2; i++) {
                pool.execute(() -> {
                    running.countDown();
                    awaitQuietly(gate);
                });
            }
            assertThat(running.await(5, TimeUnit.SECONDS)).isTrue();
            for (int i = 0; i < 100; i++) {
                pool.execute(() -> awaitQuietly(gate));
            }
            String caller = Thread.currentThread().getName();
            AtomicInteger ranOnCaller = new AtomicInteger();

            // when: 한 건 더 넣는다
            pool.execute(() -> {
                if (Thread.currentThread().getName().equals(caller)) {
                    ranOnCaller.incrementAndGet();
                }
            });

            // then: 호출 스레드(모집 토글 트랜잭션)에서 돌지 않는다
            assertThat(ranOnCaller).hasValue(0);
        } finally {
            gate.countDown();
            pool.shutdown();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
