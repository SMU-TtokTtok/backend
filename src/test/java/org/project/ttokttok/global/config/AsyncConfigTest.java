package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
}
