package org.project.ttokttok.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 메일 발송 전용 유계 스레드풀.
     *
     * <p>@EnableAsync 기본 executor(SimpleAsyncTaskExecutor)는 요청마다 새 스레드를
     * 무제한 생성한다. 이름 지정 풀로 메일 발송을 격리해, 외부 SMTP의 동시 연결 제한을
     * 넘지 않도록 상한을 둔다. 메일은 CPU가 아닌 네트워크 I/O 대기 작업이라 코어 수가
     * 아닌 SMTP가 감당 가능한 동시 연결 수를 기준으로 값을 잡는다.
     *
     * <p>포화 시 CallerRunsPolicy로 호출 스레드가 대신 실행(백프레셔)한다. 큐는 메모리에만 있어서
     * 종료할 때 남은 작업은 사라진다. 종료 대기(waitForTasksToCompleteOnShutdown)는 켜지 않는다.
     * 배포가 종료 신호 뒤 10초에 강제 종료하므로 보내는 양이 늘지 않았고, 종료 직후 들어온 작업을
     * 버리는 경우가 생겼다(#435 측정, load-test/results/issue-435). 대신 버리는 작업 수를 경고 로그로 남긴다.
     */
    @Bean("mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor() {
            @Override
            public void shutdown() {
                ThreadPoolExecutor pool = getThreadPoolExecutor();
                int active = pool.getActiveCount();
                int queued = pool.getQueue().size();
                super.shutdown();
                if (queued > 0 || active > 0) {
                    log.warn("메일 실행기 종료 - 대기 중이던 메일 작업 {}건을 보내지 않고 버림, 실행 중이던 {}건은 중단 신호를 받음",
                            queued, active);
                }
            }
        };
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

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
