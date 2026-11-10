package org.project.ttokttok.infrastructure.firebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.dto.FCMRequest;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * #435 U4. 이름 없는 {@code @Async}였던 FCM 알림이 실제 애플리케이션 컨텍스트에서 어느 실행기의 스레드로 도는지 확인한다.
 *
 * <p>{@link FCMService}는 진짜 빈(@Async 프록시)을 쓰고, 외부 호출인 {@link FirebaseMessaging}만 가짜로 바꾼다.
 */
@SpringBootTest
@ActiveProfiles("test")
class FCMNotificationExecutorTest {

    @Autowired private FCMService fcmService;
    @MockitoBean private FirebaseMessaging firebaseMessaging;
    @MockitoBean private EmailService emailService;
    @MockitoBean private S3Service s3Service;

    @Test
    @DisplayName("모집 알림은 FCM 전용 실행기(fcm- 스레드)에서 보낸다 (#435)")
    void sendsNotificationOnFcmExecutor() throws Exception {
        // given
        Set<String> threads = ConcurrentHashMap.newKeySet();
        CountDownLatch sent = new CountDownLatch(1);
        given(firebaseMessaging.sendEachForMulticast(any())).willAnswer(inv -> {
            threads.add(Thread.currentThread().getName());
            sent.countDown();
            return mock(BatchResponse.class);
        });

        // when
        fcmService.sendNotification(new FCMRequest(List.of("token-1"), "모집 시작", "지금 지원하세요"));

        // then
        assertThat(sent.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(threads).singleElement().asString().startsWith("fcm-");
    }

    /**
     * 알림 30건을 한꺼번에 보낼 때 스레드가 몇 개 생기고 동시에 몇 건이 Firebase를 부르는지 잰다.
     * Firebase 호출 한 번에 FIREBASE_DELAY_MS가 걸린다고 둔다. 결과는 TTOKTTOK_FCM_EXPERIMENT_OUT(JSON)에 남긴다.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "TTOKTTOK_FCM_EXPERIMENT_OUT", matches = ".+")
    void measureBurst() throws Exception {
        int requests = 30;
        long firebaseDelayMs = 200;
        Set<String> threads = ConcurrentHashMap.newKeySet();
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(requests);
        given(firebaseMessaging.sendEachForMulticast(any())).willAnswer(inv -> {
            threads.add(Thread.currentThread().getName());
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            Thread.sleep(firebaseDelayMs);
            inFlight.decrementAndGet();
            done.countDown();
            return mock(BatchResponse.class);
        });

        long start = System.nanoTime();
        for (int i = 0; i < requests; i++) {
            fcmService.sendNotification(new FCMRequest(List.of("token-" + i), "모집 시작", "지금 지원하세요"));
        }
        long submitMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        boolean finished = done.await(60, TimeUnit.SECONDS);
        long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requests", requests);
        result.put("firebaseDelayMs", firebaseDelayMs);
        result.put("finished", finished);
        result.put("distinctThreads", threads.size());
        result.put("threadNamePrefixes", threads.stream().map(n -> n.replaceAll("\\d+$", "")).distinct().sorted().toList());
        result.put("maxConcurrentFirebaseCalls", maxInFlight.get());
        result.put("submitMs", submitMs);
        result.put("allDoneMs", totalMs);
        File out = new File(System.getenv("TTOKTTOK_FCM_EXPERIMENT_OUT"));
        out.getAbsoluteFile().getParentFile().mkdirs();
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, result);
    }
}
