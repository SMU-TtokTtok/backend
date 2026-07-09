package org.project.ttokttok.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

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
     * <p>포화 시 CallerRunsPolicy로 호출 스레드가 대신 실행(백프레셔)하여 메일 유실을 막는다.
     */
    @Bean("mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
