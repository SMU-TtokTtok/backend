package org.project.ttokttok.global.config;

import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    // 결과 메일 워커(fixedDelay라 자기 자신과는 겹치지 않음)가 SMTP 장애 중 스레드를 오래 잡아도
    // 04시 크론들이 쓸 스레드가 하나 남게 한다(#465). 모든 환경에서 지켜야 할 조건이라 코드에 둔다.
    // 이 값이 spring.task.scheduling.pool.size 설정보다 우선한다.
    static final int SCHEDULER_POOL_SIZE = 2;

    @Bean
    public ThreadPoolTaskSchedulerCustomizer schedulerPoolSizeCustomizer() {
        return scheduler -> scheduler.setPoolSize(SCHEDULER_POOL_SIZE);
    }
}
