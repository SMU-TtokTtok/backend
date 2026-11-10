package org.project.ttokttok.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// 결과 메일은 #460부터 DB 발송 작업과 워커로 보내므로 메일 전용 실행기는 없다.
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 모집 알림(FCM) 전용 유계 스레드풀.
     *
     * <p>이름 없는 {@code @Async}는 Spring 기본 실행기(SimpleAsyncTaskExecutor)로 가서 알림마다 새 스레드를 만들었다.
     * 30건을 한꺼번에 보내면 스레드 30개가 동시에 Firebase를 불렀다(#435 측정). 알림은 모집을 시작할 때만 나가는 드문 작업이라
     * 스레드 2개로 제한한다.
     *
     * <p>가득 차면 알림을 버리고 경고를 남긴다. 알림은 모집 토글 트랜잭션 안에서 등록되므로, 호출 스레드에서 실행하면
     * 그동안 DB 커넥션을 쥐고, 예외를 던지면 모집 토글이 롤백된다. 알림은 최선 노력(best effort)으로 보낸다.
     */
    @Bean("fcmExecutor")
    public Executor fcmExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("fcm-");
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("FCM 실행기가 가득 차 알림 작업 1건을 보내지 않고 버림 - 실행 중 {}건, 대기 {}건",
                        pool.getActiveCount(), pool.getQueue().size()));
        executor.initialize();
        return executor;
    }
}
