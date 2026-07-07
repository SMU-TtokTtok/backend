package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * {@code @Async("mailExecutor")}가 실제로 프록시를 통해 별도 스레드에서 실행되는지 검증한다.
 *
 * <p>과거 버그(self-invocation + protected)에서는 호출 스레드에서 동기 실행됐다.
 * 본 테스트는 EmailService.sendResultMail과 동일한 애노테이션 조합(다른 빈에서 호출되는
 * public @Async 메서드)이 mail- 풀 스레드로 넘어가 호출 스레드를 블로킹하지 않음을 증명한다.
 * DB/Redis/SMTP 없이 최소 컨텍스트만 띄운다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AsyncConfig.class, MailAsyncDispatchTest.AsyncProbe.class})
@DisplayName("메일 비동기 디스패치")
class MailAsyncDispatchTest {

    @Autowired
    private AsyncProbe probe;

    @Test
    @DisplayName("mailExecutor 지정 @Async 메서드는 호출 스레드가 아닌 mail- 풀 스레드에서 실행된다")
    void runsOnMailPoolThreadNotCaller() throws Exception {
        // given
        String callerThread = Thread.currentThread().getName();

        // when
        CompletableFuture<String> future = probe.captureExecutingThread();
        String workerThread = future.get(2, TimeUnit.SECONDS);

        // then
        assertThat(workerThread).startsWith("mail-");
        assertThat(workerThread).isNotEqualTo(callerThread);
    }

    @Component
    static class AsyncProbe {

        @Async("mailExecutor")
        public CompletableFuture<String> captureExecutingThread() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }
    }
}
