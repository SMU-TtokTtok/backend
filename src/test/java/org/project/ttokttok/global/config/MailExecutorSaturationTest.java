package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * {@code mailExecutor}의 동시 실행 상한(max=5)과 포화 시 백프레셔(CallerRunsPolicy)가
 * 런타임에 실제로 동작하는지 검증한다.
 *
 * <p>{@link AsyncConfigTest}는 setter로 넣은 <b>설정값</b>을 되읽을 뿐이고,
 * {@code MailAsyncDispatchTest}는 태스크 1건이 {@code mail-} 스레드로 넘어가는지만 본다.
 * 둘 다 상한이 지켜지는지는 증명하지 못한다. 이 테스트가 그 구멍을 막는다.
 *
 * <p><b>왜 105건인가.</b> {@link java.util.concurrent.ThreadPoolExecutor}는 큐가 가득 찬
 * 뒤에야 core를 넘어 스레드를 늘린다. 따라서 "20건 제출 후 스레드 &le; 5" 같은 순진한 테스트는
 * 항상 2개(core)만 관찰하며, max를 50으로 바꿔도 통과하는 위양성이 된다. 실제 상한을 목격하려면
 * 큐를 먼저 채워야 한다:
 *
 * <pre>
 *   제출 1~2     → core 스레드 2개 생성
 *   제출 3~102   → 전부 큐 적재 (스레드 증설 없음)
 *   제출 103~105 → 큐 포화 → 스레드 3, 4, 5 생성 (max 도달)
 *   제출 106     → 큐 만석 + max 도달 → 거부 → CallerRunsPolicy → 호출 스레드가 직접 실행
 * </pre>
 *
 * <p><b>이 상한이 제한하는 대상.</b> 태스크 1개 = {@code EmailService#sendResultMail} 호출 1회다.
 * 이 메서드는 {@code List<String>}을 받아 내부에서 순회하므로, 200통을 발송해도 태스크는 1개이고
 * SMTP 전송은 그 태스크 안에서 순차로 일어난다({@code ApplicantAdminService}는 합격/불합격
 * 두 번만 호출한다). 따라서 max=5가 제한하는 것은 <b>동시에 진행 중인 벌크 발송 요청 수</b>이지
 * 메일 통수가 아니다. 메일 통수는 이 풀의 동시성과 무관하다.
 *
 * <p>{@code AsyncConfig}의 상한 값을 바꾸면 이 테스트는 깨진다. 계약을 바꾸는 변경에는
 * 위 표와 아래 상수도 함께 갱신해야 한다는 신호다.
 *
 * <p>Spring 컨텍스트 없이 빈 생성 메서드를 직접 호출한다({@link AsyncConfigTest}와 동일).
 * DB/Redis/SMTP가 필요 없고 밀리초 단위로 끝난다.
 */
@DisplayName("mailExecutor 포화 동작")
class MailExecutorSaturationTest {

    private static final int MAX_POOL_SIZE = 5;
    private static final int QUEUE_CAPACITY = 100;

    /** 풀이 거부 없이 받아낼 수 있는 최대 태스크 수 = 실행 중 5건 + 큐 대기 100건. */
    private static final int SATURATING_TASKS = MAX_POOL_SIZE + QUEUE_CAPACITY;

    private static final int TIMEOUT_SECONDS = 5;

    @Test
    @Timeout(30) // 래치 설계가 틀어졌을 때 무한 대기 대신 실패시키는 안전장치
    @DisplayName("풀이 포화되면 스레드는 5개에서 멈추고, 초과분은 호출 스레드가 실행한다")
    void saturatedPoolCapsAtMaxThenRunsOverflowOnCaller() throws Exception {
        // given
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) new AsyncConfig().mailExecutor();
        CountDownLatch gate = new CountDownLatch(1);                 // 워커를 동시에 붙잡아 두는 관문
        CountDownLatch allWorkersStarted = new CountDownLatch(MAX_POOL_SIZE);
        Set<String> workerThreads = ConcurrentHashMap.newKeySet();

        try {
            // when: 거부 직전까지 가득 채운다 (2 core + 100 큐 + 3 증설 = 105건)
            for (int i = 0; i < SATURATING_TASKS; i++) {
                pool.execute(() -> {
                    workerThreads.add(Thread.currentThread().getName());
                    allWorkersStarted.countDown();
                    awaitQuietly(gate); // 5개가 동시에 살아있도록 붙잡아 둔다
                });
            }

            // then: 정확히 5개 스레드만 동시에 살아있고, 전부 mail- 풀 소속이다
            assertThat(allWorkersStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(workerThreads)
                    .hasSize(MAX_POOL_SIZE)
                    .allMatch(name -> name.startsWith("mail-"));
            assertThat(pool.getPoolSize()).isEqualTo(MAX_POOL_SIZE);

            // and: 나머지는 스레드를 늘리는 대신 큐에 쌓여 있다 (큐 선적재 동작)
            assertThat(pool.getThreadPoolExecutor().getQueue()).hasSize(QUEUE_CAPACITY);

            // and: 한 건 더 밀어 넣으면 거부되어 CallerRunsPolicy가 호출 스레드에서 동기 실행한다
            AtomicReference<String> overflowRunner = new AtomicReference<>();
            pool.execute(() -> overflowRunner.set(Thread.currentThread().getName()));
            // ^ 이 태스크는 gate를 기다리면 안 된다. 호출 스레드에서 동기 실행되므로
            //   테스트 스레드가 자기 자신을 데드락시킨다.

            assertThat(overflowRunner.get())
                    .isEqualTo(Thread.currentThread().getName())
                    .doesNotStartWith("mail-");
        } finally {
            gate.countDown(); // 단언이 실패해도 워커를 반드시 풀어준다
            pool.shutdown();  // mail- 스레드 5개 누수 방지
        }
    }

    /**
     * 람다 안에서는 checked exception을 던질 수 없어 감싼다.
     * 무기한 대기하면 {@code finally}가 실패했을 때 워커가 영영 남으므로 상한을 둔다.
     */
    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
