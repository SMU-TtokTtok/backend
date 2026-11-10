package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@DisplayName("AsyncConfig")
class AsyncConfigTest {

    @Test
    @DisplayName("mailExecutor는 core=2, max=5, prefix=\"mail-\"의 유계 스레드풀이다")
    void mailExecutorIsBoundedPool() {
        // given
        Executor executor = new AsyncConfig().mailExecutor();

        // then
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        assertThat(pool.getCorePoolSize()).isEqualTo(2);
        assertThat(pool.getMaxPoolSize()).isEqualTo(5);
        assertThat(pool.getThreadNamePrefix()).isEqualTo("mail-");
    }

    @Test
    @DisplayName("mailExecutor의 큐 용량은 100이다")
    void mailExecutorQueueCapacityIsHundred() {
        // given
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) new AsyncConfig().mailExecutor();

        // then: 초기화 직후 빈 큐의 잔여 용량 == 설정한 queueCapacity
        assertThat(pool.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(100);
    }

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

    @Test
    @Timeout(10)
    @DisplayName("종료할 때 보내지 못하고 버리는 대기 작업 수와 실행 중이던 작업 수를 경고 로그로 남긴다 (#435)")
    void logsDiscardedTasksOnShutdown() throws Exception {
        // given: 스레드 2개는 붙잡아 두고, 작업 3개는 큐에서 기다리게 한다
        Logger logger = (Logger) LoggerFactory.getLogger(AsyncConfig.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) new AsyncConfig().mailExecutor();
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch running = new CountDownLatch(2);
        AtomicInteger queuedRan = new AtomicInteger();
        try {
            for (int i = 0; i < 2; i++) {
                pool.execute(() -> {
                    running.countDown();
                    awaitQuietly(gate);
                });
            }
            assertThat(running.await(5, TimeUnit.SECONDS)).isTrue();
            for (int i = 0; i < 3; i++) {
                pool.execute(queuedRan::incrementAndGet);
            }

            // when
            pool.shutdown();

            // then: 대기하던 3건은 실행되지 않고, 그 수가 로그에 남는다
            assertThat(queuedRan).hasValue(0);
            assertThat(appender.list)
                    .filteredOn(event -> event.getLevel() == Level.WARN)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("메일 실행기 종료 - 대기 중이던 메일 작업 3건을 보내지 않고 버림, 실행 중이던 2건은 중단 신호를 받음");
        } finally {
            gate.countDown();
            logger.detachAppender(appender);
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
