package org.project.ttokttok.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * 인증코드 메일(SMTP)이 트랜잭션 안에서 동기 실행될 때 DB 커넥션 점유와 다른 API 영향을 재는 실험.
 * 실제 SMTP 대신 지연을 넣은 mock을 쓴다. 전용 로컬 PostgreSQL에서만 실행한다.
 * 결과는 TTOKTTOK_EXPERIMENT_OUT(JSON)에 남긴다. 단언 없이 측정만 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=verification_tx_hold_experiment",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.datasource.hikari.connection-init-sql=SET search_path TO verification_tx_hold_experiment",
        "spring.datasource.hikari.maximum-pool-size=10",
        "spring.datasource.hikari.connection-timeout=30000",
        "spring.flyway.enabled=false",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF",
        "logging.level.org.project.ttokttok=WARN",
        "logging.level.org.springframework.jdbc.core=OFF"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_CONCURRENCY_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:\\d+/ttokttok426")
class VerificationMailTxHoldExperimentTest {

    private static final int SENDS = Integer.parseInt(System.getenv().getOrDefault("TTOKTTOK_SENDS", "30"));
    private static final int REPETITIONS = Integer.parseInt(System.getenv().getOrDefault("TTOKTTOK_REPETITIONS", "3"));
    private static final int PROBE_THREADS = 4;
    private static final long BASELINE_MS = 2000;
    private static final long SAMPLE_INTERVAL_MS = 50;
    private static final List<Long> DELAYS_MS = Arrays.stream(
            System.getenv().getOrDefault("TTOKTTOK_MAIL_DELAYS_MS", "2000,10000").split(","))
            .map(Long::parseLong).toList();

    // Hikari가 빌려준 커넥션마다 반납까지 걸린 시간·획득 대기 시간·타임아웃을 모은다.
    private static final ConcurrentLinkedQueue<Long> USAGE_MS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Long> SEND_USAGE_MS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Long> PROBE_USAGE_MS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Long> ACQUIRE_NANOS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger TIMEOUTS = new AtomicInteger();

    @TestConfiguration
    static class PoolMetrics {
        @Bean
        static BeanPostProcessor hikariUsageRecorder() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) {
                    if (bean instanceof HikariDataSource ds) {
                        ds.setMetricsTrackerFactory((pool, stats) -> new IMetricsTracker() {
                            @Override public void recordConnectionAcquiredNanos(long nanos) { ACQUIRE_NANOS.add(nanos); }
                            @Override public void recordConnectionUsageMillis(long millis) { recordUsage(millis); }
                            @Override public void recordConnectionTimeout() { TIMEOUTS.incrementAndGet(); }
                        });
                    }
                    return bean;
                }
            };
        }
    }

    @LocalServerPort private int port;
    @Autowired private HikariDataSource dataSource;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EmailVerificationRepository emailVerificationRepository;
    @MockitoBean private EmailService emailService;
    @MockitoBean private S3Service s3Service;
    @MockitoBean private FCMService fcmService;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        String url = System.getenv("TTOKTTOK_CONCURRENCY_JDBC_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:\\d+/ttokttok426")) {
            throw new IllegalArgumentException("Use the dedicated local ttokttok426 test database only");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    void measure() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conditions", Map.of("sends", SENDS, "repetitions", REPETITIONS, "probeThreads", PROBE_THREADS,
                "baselineMs", BASELINE_MS, "sampleIntervalMs", SAMPLE_INTERVAL_MS,
                "hikariMaximumPoolSize", dataSource.getMaximumPoolSize(),
                "hikariConnectionTimeoutMs", dataSource.getConnectionTimeout()));

        warmUp();
        Map<String, Object> http = new LinkedHashMap<>();
        for (long delay : DELAYS_MS) {
            List<Object> reps = new ArrayList<>();
            for (int rep = 0; rep < REPETITIONS; rep++) {
                reps.add(runHttpRepetition(delay));
            }
            http.put("mailDelayMs=" + delay, reps);
        }
        result.put("httpSendVerification", http);
        result.put("mechanism", runMechanism(DELAYS_MS.get(0)));

        File out = new File(System.getenv().getOrDefault("TTOKTTOK_EXPERIMENT_OUT", "build/verification-tx-hold.json"));
        out.getAbsoluteFile().getParentFile().mkdirs();
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, result);
    }

    private void warmUp() throws Exception {
        given(emailService.isValidSangmyungEmail(anyString())).willReturn(true);
        given(emailService.sendVerificationCode(anyString())).willReturn("123456");
        for (int i = 0; i < 20; i++) {
            sendVerification(uniqueEmail());
            probe();
        }
    }

    /** 인증코드 발송 SENDS개를 동시에 보내고, 그동안 다른 API(공지 목록)를 계속 호출한다. */
    private Map<String, Object> runHttpRepetition(long delayMs) throws Exception {
        given(emailService.sendVerificationCode(anyString())).willAnswer(inv -> {
            Thread.sleep(delayMs);
            return "123456";
        });

        ConcurrentLinkedQueue<long[]> probes = new ConcurrentLinkedQueue<>(); // {startNanos, latencyNanos, status}
        AtomicBoolean stopProbes = new AtomicBoolean();
        ExecutorService pool = Executors.newFixedThreadPool(SENDS + PROBE_THREADS);
        Sampler sampler = new Sampler();
        sampler.start();
        long t0 = System.nanoTime();
        for (int i = 0; i < PROBE_THREADS; i++) {
            pool.submit(() -> {
                while (!stopProbes.get()) {
                    long s = System.nanoTime();
                    int status = probe();
                    probes.add(new long[]{s, System.nanoTime() - s, status});
                }
                return null;
            });
        }
        Thread.sleep(BASELINE_MS);
        resetPoolMetrics();

        CountDownLatch start = new CountDownLatch(1);
        long loadStart = System.nanoTime();
        List<Future<long[]>> sends = new ArrayList<>();
        for (int i = 0; i < SENDS; i++) {
            String email = uniqueEmail();
            sends.add(pool.submit(() -> {
                start.await();
                long s = System.nanoTime();
                int status = sendVerification(email);
                return new long[]{System.nanoTime() - s, status};
            }));
        }
        start.countDown();
        List<long[]> sendResults = new ArrayList<>();
        for (Future<long[]> f : sends) {
            sendResults.add(f.get(5, TimeUnit.MINUTES));
        }
        long loadEnd = System.nanoTime();
        List<Long> usage = new ArrayList<>(USAGE_MS);
        List<Long> sendUsage = new ArrayList<>(SEND_USAGE_MS);
        List<Long> probeUsage = new ArrayList<>(PROBE_USAGE_MS);
        List<Long> acquire = new ArrayList<>(ACQUIRE_NANOS);
        int timeouts = TIMEOUTS.get();
        stopProbes.set(true);
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.MINUTES);
        sampler.stop();

        List<long[]> baseline = probes.stream().filter(p -> p[0] - t0 >= 0 && p[0] < loadStart).toList();
        List<long[]> during = probes.stream().filter(p -> p[0] >= loadStart && p[0] < loadEnd).toList();

        Map<String, Object> rep = new LinkedHashMap<>();
        rep.put("mailDelayMs", delayMs);
        rep.put("loadWallMs", ms(loadEnd - loadStart));
        rep.put("sendLatencyMs", stats(sendResults.stream().map(r -> r[0]).toList(), VerificationMailTxHoldExperimentTest::ms));
        rep.put("sendStatus", statusCounts(sendResults.stream().map(r -> r[1]).toList()));
        rep.put("probeBaselineLatencyMs", stats(baseline.stream().map(p -> p[1]).toList(), VerificationMailTxHoldExperimentTest::ms));
        rep.put("probeBaselineStatus", statusCounts(baseline.stream().map(p -> p[2]).toList()));
        rep.put("probeDuringLatencyMs", stats(during.stream().map(p -> p[1]).toList(), VerificationMailTxHoldExperimentTest::ms));
        rep.put("probeDuringStatus", statusCounts(during.stream().map(p -> p[2]).toList()));
        rep.put("connectionUsageMs", stats(usage, Long::doubleValue));
        rep.put("sendConnectionUsageMs", stats(sendUsage, Long::doubleValue));
        rep.put("probeConnectionUsageMs", stats(probeUsage, Long::doubleValue));
        rep.put("connectionAcquireWaitMs", stats(acquire, VerificationMailTxHoldExperimentTest::ms));
        rep.put("connectionTimeouts", timeouts);
        rep.putAll(sampler.summary());
        return rep;
    }

    /**
     * 같은 DB 작업(미인증 코드 만료 + 새 코드 저장)과 지연을 세 위치에 두고 커넥션 점유 시간을 비교한다.
     * IN_TX는 기존 코드, AFTER_COMMIT은 커밋 후 훅으로 옮기는 대안, BEFORE_TX는 트랜잭션 전에 실행하는 경우다.
     */
    private Map<String, Object> runMechanism(long delayMs) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String placement : List.of("IN_TX", "AFTER_COMMIT", "BEFORE_TX")) {
            List<Object> reps = new ArrayList<>();
            for (int rep = 0; rep < REPETITIONS; rep++) {
                resetPoolMetrics();
                Sampler sampler = new Sampler();
                sampler.start();
                ExecutorService pool = Executors.newFixedThreadPool(dataSource.getMaximumPoolSize());
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Long>> tasks = new ArrayList<>();
                for (int i = 0; i < dataSource.getMaximumPoolSize(); i++) {
                    String email = uniqueEmail();
                    tasks.add(pool.submit(() -> {
                        start.await();
                        long s = System.nanoTime();
                        runPlacement(placement, email, delayMs);
                        return System.nanoTime() - s;
                    }));
                }
                start.countDown();
                List<Long> latency = new ArrayList<>();
                for (Future<Long> f : tasks) {
                    latency.add(f.get(2, TimeUnit.MINUTES));
                }
                pool.shutdown();
                sampler.stop();
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("taskLatencyMs", stats(latency, VerificationMailTxHoldExperimentTest::ms));
                r.put("connectionUsageMs", stats(new ArrayList<>(USAGE_MS), Long::doubleValue));
                r.putAll(sampler.summary());
                reps.add(r);
            }
            out.put(placement, reps);
        }
        return out;
    }

    private void runPlacement(String placement, String email, long delayMs) throws InterruptedException {
        if (placement.equals("BEFORE_TX")) {
            Thread.sleep(delayMs);
        }
        transactionTemplate.executeWithoutResult(status -> {
            emailVerificationRepository.expireAllPendingVerifications(email);
            emailVerificationRepository.save(EmailVerification.builder()
                    .email(email).code("123456").expiresAt(LocalDateTime.now().plusMinutes(5)).build());
            if (placement.equals("IN_TX")) {
                sleep(delayMs);
            }
            if (placement.equals("AFTER_COMMIT")) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sleep(delayMs);
                    }
                });
            }
        });
    }

    private int sendVerification(String email) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/user/auth/send-verification"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + email + "\"}"))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private int probe() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/notices"))
                    .timeout(Duration.ofSeconds(120)).GET().build();
            return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (Exception e) {
            return -1;
        }
    }

    /** 커넥션을 반납한 요청 스레드의 URI로 발송 요청과 공지 조회의 점유 시간을 나눈다. */
    private static void recordUsage(long millis) {
        USAGE_MS.add(millis);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String uri = attrs.getRequest().getRequestURI();
            (uri.endsWith("/send-verification") ? SEND_USAGE_MS : PROBE_USAGE_MS).add(millis);
        }
    }

    private static void resetPoolMetrics() {
        USAGE_MS.clear();
        SEND_USAGE_MS.clear();
        PROBE_USAGE_MS.clear();
        ACQUIRE_NANOS.clear();
        TIMEOUTS.set(0);
    }

    private static String uniqueEmail() {
        return UUID.randomUUID().toString().substring(0, 12) + "@sangmyung.kr";
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static double ms(long nanos) {
        return Math.round(nanos / 100_000.0) / 10.0;
    }

    private static Map<String, Object> stats(Collection<Long> values, Function<Long, Double> toMs) {
        List<Long> sorted = values.stream().sorted().toList();
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("count", sorted.size());
        if (sorted.isEmpty()) {
            return s;
        }
        s.put("p50", toMs.apply(nearestRank(sorted, 0.50)));
        s.put("p95", toMs.apply(nearestRank(sorted, 0.95)));
        s.put("max", toMs.apply(sorted.get(sorted.size() - 1)));
        s.put("over1000ms", sorted.stream().filter(v -> toMs.apply(v) > 1000).count());
        return s;
    }

    private static long nearestRank(List<Long> sorted, double p) {
        return sorted.get(Math.max(0, (int) Math.ceil(p * sorted.size()) - 1));
    }

    private static Map<String, Long> statusCounts(List<Long> statuses) {
        Map<String, Long> counts = new TreeMap<>();
        statuses.forEach(s -> counts.merge(String.valueOf(s), 1L, Long::sum));
        return counts;
    }

    /** Hikari 풀 상태와 pg_stat_activity를 SAMPLE_INTERVAL_MS마다 읽는다. 풀 밖의 별도 커넥션을 쓴다. */
    private class Sampler {
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final List<int[]> pool = new ArrayList<>();      // {active, idle, pending}
        private final List<long[]> pg = new ArrayList<>();       // {idleInTxCount, maxIdleInTxMs}
        private final Map<String, Long> idleInTxQueries = new TreeMap<>();
        private Thread thread;

        void start() {
            thread = new Thread(() -> {
                try (Connection c = DriverManager.getConnection(dataSource.getJdbcUrl(), "postgres", "");
                     Statement st = c.createStatement()) {
                    while (running.get()) {
                        var mx = dataSource.getHikariPoolMXBean();
                        pool.add(new int[]{mx.getActiveConnections(), mx.getIdleConnections(), mx.getThreadsAwaitingConnection()});
                        try (ResultSet rs = st.executeQuery("""
                                select count(*) filter (where state = 'idle in transaction'),
                                       coalesce(max(extract(epoch from now() - state_change) * 1000)
                                                filter (where state = 'idle in transaction'), 0)::bigint,
                                       string_agg(left(query, 60), '|') filter (where state = 'idle in transaction')
                                from pg_stat_activity
                                where datname = current_database() and pid <> pg_backend_pid()""")) {
                            rs.next();
                            pg.add(new long[]{rs.getLong(1), rs.getLong(2)});
                            String q = rs.getString(3);
                            if (q != null) {
                                Arrays.stream(q.split("\\|")).forEach(x -> idleInTxQueries.merge(x, 1L, Long::sum));
                            }
                        }
                        Thread.sleep(SAMPLE_INTERVAL_MS);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            thread.start();
        }

        void stop() throws InterruptedException {
            running.set(false);
            thread.join();
        }

        Map<String, Object> summary() {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("samples", pool.size());
            s.put("hikariMaxActive", pool.stream().mapToInt(p -> p[0]).max().orElse(0));
            s.put("hikariMaxPending", pool.stream().mapToInt(p -> p[2]).max().orElse(0));
            s.put("samplesWithPending", pool.stream().filter(p -> p[2] > 0).count());
            s.put("samplesPoolExhausted", pool.stream().filter(p -> p[1] == 0 && p[0] >= dataSource.getMaximumPoolSize()).count());
            s.put("pgMaxIdleInTransaction", pg.stream().mapToLong(p -> p[0]).max().orElse(0));
            s.put("pgSamplesWithIdleInTransaction", pg.stream().filter(p -> p[0] > 0).count());
            s.put("pgMaxIdleInTransactionAgeMs", pg.stream().mapToLong(p -> p[1]).max().orElse(0));
            s.put("pgIdleInTransactionLastQuery", idleInTxQueries);
            return s;
        }
    }
}
