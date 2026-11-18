package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SchedulingConfigTest {

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Test
    @DisplayName("스케줄러 스레드가 2개라 메일 워커가 한 스레드를 잡아도 크론이 돌 스레드가 남는다")
    void schedulerHasTwoThreads() {
        assertThat(taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize())
                .isEqualTo(SchedulingConfig.SCHEDULER_POOL_SIZE)
                .isEqualTo(2);
    }
}
