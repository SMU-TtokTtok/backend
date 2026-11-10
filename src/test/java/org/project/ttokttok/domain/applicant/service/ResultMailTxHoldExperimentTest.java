package org.project.ttokttok.domain.applicant.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.jwt.JwtFactory;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * #435 결과 메일 API가 mailExecutor 포화 때 DB 커넥션을 얼마나 쥐는지 재는 실험.
 *
 * <p>실제 SMTP 대신 한 통에 지연을 넣은 가짜 {@link JavaMailSender}를 쓰고, {@code mailExecutor}는 실제 빈을 그대로 쓴다.
 * SATURATED는 끝나지 않는 작업 105개(스레드 5 + 큐 100)로 실행기를 채운 뒤 API를 부른다. 넘친 작업은
 * CallerRunsPolicy로 요청 스레드에서 실행된다. NORMAL은 실행기가 비어 있을 때다.
 * 전용 로컬 PostgreSQL에서만 실행한다. 결과는 TTOKTTOK_EXPERIMENT_OUT(JSON)에 남긴다. 단언 없이 측정만 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=result_mail_tx_hold_experiment",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.datasource.hikari.connection-init-sql=SET search_path TO result_mail_tx_hold_experiment",
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
class ResultMailTxHoldExperimentTest {

    // 기본값 25ms는 운영 Postfix 로그의 delays a(앱에서 받아 큐에 쓰기까지, 20~30ms) 실측값이다.
    private static final long MAIL_DELAY_MS = Long.parseLong(env("TTOKTTOK_MAIL_DELAY_MS", "25"));
    private static final int RECIPIENTS_PER_RESULT = Integer.parseInt(env("TTOKTTOK_RECIPIENTS_PER_RESULT", "20"));
    private static final int REPETITIONS = Integer.parseInt(env("TTOKTTOK_REPETITIONS", "3"));
    private static final List<Integer> CONCURRENCY = Arrays.stream(env("TTOKTTOK_CONCURRENCY", "1,5,10").split(","))
            .map(Integer::parseInt).toList();
    private static final List<String> MODES = List.of(env("TTOKTTOK_MODES", "NORMAL,SATURATED").split(","));
    private static final int PROBE_THREADS = 4;
    private static final long BASELINE_MS = 2000;
    private static final long SAMPLE_INTERVAL_MS = 50;
    private static final int EXECUTOR_CAPACITY = 105; // 실행 중 max 5 + 큐 100

    // Hikari가 빌려준 커넥션마다 반납까지 걸린 시간·획득 대기·타임아웃. 점유 시간은 {millis, 출처}로 한 큐에 모은다.
    private static final int OTHER = 0, SEND = 1, PROBE = 2;
    private static final ConcurrentLinkedQueue<long[]> USAGE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Long> ACQUIRE_NANOS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger TIMEOUTS = new AtomicInteger();

    // 가짜 발송기가 어느 스레드에서 몇 통을 보냈는지 센다.
    private static final AtomicInteger SENT_ON_MAIL_THREAD = new AtomicInteger();
    private static final AtomicInteger SENT_ON_OTHER_THREAD = new AtomicInteger();
    private static final AtomicBoolean FAST_DRAIN = new AtomicBoolean();

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
    @Autowired private EntityManager entityManager;
    @Autowired private JwtFactory jwtFactory;
    @Autowired @Qualifier("mailExecutor") private java.util.concurrent.Executor mailExecutor;
    @MockitoBean private JavaMailSender mailSender;
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

    private record ClubFixture(String clubId, String token) { }

    @Test
    void measure() throws Exception {
        stubMailSender();
        int maxConcurrency = CONCURRENCY.stream().mapToInt(Integer::intValue).max().orElse(1);
        List<ClubFixture> clubs = new ArrayList<>();
        for (int i = 0; i < maxConcurrency; i++) {
            clubs.add(createClub(i));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conditions", conditions());
        warmUp(clubs.get(0));

        for (String mode : MODES) {
            Map<String, Object> byConcurrency = new LinkedHashMap<>();
            for (int concurrency : CONCURRENCY) {
                List<Object> reps = new ArrayList<>();
                for (int rep = 0; rep < REPETITIONS; rep++) {
                    System.out.println("[435] " + mode + " concurrency=" + concurrency + " rep=" + rep + " start " + executorState());
                    reps.add(runRepetition(mode, clubs.subList(0, concurrency)));
                    System.out.println("[435] " + mode + " concurrency=" + concurrency + " rep=" + rep + " done " + executorState());
                }
                byConcurrency.put("concurrency=" + concurrency, reps);
            }
            result.put(mode, byConcurrency);
        }

        File out = new File(env("TTOKTTOK_EXPERIMENT_OUT", "build/result-mail-tx-hold.json"));
        out.getAbsoluteFile().getParentFile().mkdirs();
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, result);
    }

    private Map<String, Object> conditions() {
        ThreadPoolTaskExecutor pool = pool();
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("mailDelayMs", MAIL_DELAY_MS);
        c.put("recipientsPerResult", RECIPIENTS_PER_RESULT);
        c.put("recipientsPerRequest", RECIPIENTS_PER_RESULT * 2);
        c.put("repetitions", REPETITIONS);
        c.put("concurrency", CONCURRENCY);
        c.put("probeThreads", PROBE_THREADS);
        c.put("baselineMs", BASELINE_MS);
        c.put("sampleIntervalMs", SAMPLE_INTERVAL_MS);
        c.put("hikariMaximumPoolSize", dataSource.getMaximumPoolSize());
        c.put("hikariConnectionTimeoutMs", dataSource.getConnectionTimeout());
        c.put("mailExecutor", Map.of("core", pool.getCorePoolSize(), "max", pool.getMaxPoolSize(),
                "queueCapacity", pool.getQueueCapacity()));
        c.put("postgres", postgresVersion());
        c.put("java", System.getProperty("java.version"));
        return c;
    }

    /** 한 통에 MAIL_DELAY_MS가 걸리는 가짜 SMTP. FAST_DRAIN이면 측정이 끝난 뒤 남은 작업을 바로 비운다. */
    private void stubMailSender() {
        given(mailSender.createMimeMessage()).willAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        willAnswer(inv -> {
            if (Thread.currentThread().getName().startsWith("mail-")) {
                SENT_ON_MAIL_THREAD.incrementAndGet();
            } else {
                SENT_ON_OTHER_THREAD.incrementAndGet();
            }
            if (!FAST_DRAIN.get()) {
                Thread.sleep(MAIL_DELAY_MS);
            }
            return null;
        }).given(mailSender).send(any(MimeMessage.class));
    }

    /** 관리자 1명, 활성 폼 1개, 서류 합격·불합격 지원자를 RECIPIENTS_PER_RESULT명씩 만든다. */
    private ClubFixture createClub(int index) {
        return transactionTemplate.execute(status -> {
            Admin admin = Admin.adminJoin("mailexp" + index, "experiment-password");
            entityManager.persist(admin);
            Club club = Club.builder().admin(admin).clubName("메일 실험 동아리 " + index)
                    .clubUniv(ClubUniv.ENGINEERING).build();
            entityManager.persist(club);
            ApplyForm form = ApplyForm.createApplyForm(club, true,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(11), LocalDate.now().plusDays(12), 100,
                    Set.of(ApplicableGrade.FIRST_GRADE), "실험 폼 " + index, "부제", List.of());
            entityManager.persist(form);
            for (int i = 0; i < RECIPIENTS_PER_RESULT * 2; i++) {
                String id = "c" + index + "a" + i;
                Applicant applicant = Applicant.createApplicant(id + "@sangmyung.kr", "지원자 " + id, 20, "컴퓨터공학",
                        id + "@example.com", "01012345678", StudentStatus.ENROLLED, Grade.FIRST_GRADE, Gender.MALE, form);
                applicant.submitDocument(List.of());
                if (i < RECIPIENTS_PER_RESULT) {
                    applicant.passDocumentEvaluation();
                } else {
                    applicant.failDocumentEvaluation();
                }
                entityManager.persist(applicant);
            }
            return new ClubFixture(club.getId(), "Bearer " + jwtFactory.generateValidToken(admin.getUsername(), Role.ROLE_ADMIN));
        });
    }

    private void warmUp(ClubFixture club) throws Exception {
        FAST_DRAIN.set(true);
        for (int i = 0; i < 20; i++) {
            sendResultMail(club);
            probe();
        }
        awaitExecutorIdle();
        FAST_DRAIN.set(false);
    }

    /**
     * 동아리 concurrency곳이 동시에 결과 메일을 보내고, 그동안 다른 API(공지 목록)를 계속 호출한다.
     * SATURATED는 그 전에 끝나지 않는 작업으로 실행기를 가득 채운다.
     */
    private Map<String, Object> runRepetition(String mode, List<ClubFixture> clubs) throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        Map<String, Object> executorBefore = mode.equals("SATURATED") ? saturate(gate) : executorState();

        ConcurrentLinkedQueue<long[]> probes = new ConcurrentLinkedQueue<>(); // {startNanos, latencyNanos, status}
        AtomicBoolean stopProbes = new AtomicBoolean();
        ExecutorService pool = Executors.newFixedThreadPool(clubs.size() + PROBE_THREADS);
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
        SENT_ON_MAIL_THREAD.set(0);
        SENT_ON_OTHER_THREAD.set(0);

        CountDownLatch start = new CountDownLatch(1);
        long loadStart = System.nanoTime();
        List<Future<long[]>> sends = new ArrayList<>();
        for (ClubFixture club : clubs) {
            sends.add(pool.submit(() -> {
                start.await();
                long s = System.nanoTime();
                int status = sendResultMail(club);
                return new long[]{System.nanoTime() - s, status};
            }));
        }
        start.countDown();
        List<long[]> sendResults = new ArrayList<>();
        for (Future<long[]> f : sends) {
            sendResults.add(f.get(10, TimeUnit.MINUTES));
        }
        long loadEnd = System.nanoTime();
        List<long[]> usage = new ArrayList<>(USAGE);
        List<Long> acquire = new ArrayList<>(ACQUIRE_NANOS);
        int timeouts = TIMEOUTS.get();
        int sentOnRequestThread = SENT_ON_OTHER_THREAD.get();
        int sentOnMailThreadDuringLoad = SENT_ON_MAIL_THREAD.get();
        stopProbes.set(true);
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.MINUTES);
        sampler.stop();

        FAST_DRAIN.set(true);
        gate.countDown();
        awaitExecutorIdle();
        FAST_DRAIN.set(false);

        List<long[]> baseline = probes.stream().filter(p -> p[0] - t0 >= 0 && p[0] < loadStart).toList();
        List<long[]> during = probes.stream().filter(p -> p[0] >= loadStart && p[0] < loadEnd).toList();

        Map<String, Object> rep = new LinkedHashMap<>();
        rep.put("mode", mode);
        rep.put("concurrency", clubs.size());
        rep.put("executorBeforeLoad", executorBefore);
        rep.put("loadWallMs", ms(loadEnd - loadStart));
        rep.put("sendLatencyMs", stats(sendResults.stream().map(r -> r[0]).toList(), ResultMailTxHoldExperimentTest::ms));
        rep.put("sendStatus", statusCounts(sendResults.stream().map(r -> r[1]).toList()));
        rep.put("mailsSentOnRequestThread", sentOnRequestThread);
        rep.put("mailsSentOnMailThreadDuringLoad", sentOnMailThreadDuringLoad);
        rep.put("probeBaselineLatencyMs", stats(baseline.stream().map(p -> p[1]).toList(), ResultMailTxHoldExperimentTest::ms));
        rep.put("probeBaselineStatus", statusCounts(baseline.stream().map(p -> p[2]).toList()));
        rep.put("probeDuringLatencyMs", stats(during.stream().map(p -> p[1]).toList(), ResultMailTxHoldExperimentTest::ms));
        rep.put("probeDuringStatus", statusCounts(during.stream().map(p -> p[2]).toList()));
        rep.put("connectionUsageMs", stats(usageOf(usage, -1), Long::doubleValue));
        rep.put("sendConnectionUsageMs", stats(usageOf(usage, SEND), Long::doubleValue));
        rep.put("probeConnectionUsageMs", stats(usageOf(usage, PROBE), Long::doubleValue));
        rep.put("connectionAcquireWaitMs", stats(acquire, ResultMailTxHoldExperimentTest::ms));
        rep.put("connectionTimeouts", timeouts);
        rep.putAll(sampler.summary());
        return rep;
    }

    /**
     * 끝나지 않는 작업으로 실행기를 채운다. 스레드 5개가 모두 바쁘고 큐 100칸이 찰 때까지 넣는다.
     *
     * <p>쉬고 있던 스레드가 남아 있으면 작업은 큐에 들어갔다가 그 스레드가 꺼내 간다. 꺼내 가기 전에 다음 작업을
     * 넣으면 큐가 먼저 차서 거절되므로, 하나 넣을 때마다 쉬는 스레드가 가져갈 때까지 기다린다.
     * 그래도 거절되면 CallerRunsPolicy로 이 스레드에서 실행되므로, mail- 스레드가 아니면 바로 끝낸다.
     */
    private Map<String, Object> saturate(CountDownLatch gate) throws InterruptedException {
        ThreadPoolTaskExecutor pool = pool();
        Map<String, Object> atStart = executorState();
        AtomicInteger rejected = new AtomicInteger();
        Runnable blocker = () -> {
            if (!Thread.currentThread().getName().startsWith("mail-")) {
                rejected.incrementAndGet();
                return;
            }
            try {
                gate.await(15, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        int submitted = 0;
        while (!saturated(pool)) {
            if (++submitted > EXECUTOR_CAPACITY * 3) {
                Map<String, Object> state = executorState();
                state.put("submitted", submitted);
                state.put("rejected", rejected.get());
                state.put("atStart", atStart);
                gate.countDown();
                throw new IllegalStateException("mailExecutor was not saturated: " + state);
            }
            pool.execute(blocker);
            awaitIdleWorkersPickedUp(pool);
        }
        Map<String, Object> state = executorState();
        state.put("atStart", atStart);
        state.put("blockersSubmitted", submitted);
        state.put("blockersRejectedDuringSetup", rejected.get());
        return state;
    }

    /** 쉬는 스레드가 있고 큐에 작업이 남아 있으면, 스레드가 작업을 가져갈 때까지 최대 1초 기다린다. */
    private void awaitIdleWorkersPickedUp(ThreadPoolTaskExecutor pool) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (pool.getActiveCount() < pool.getPoolSize()
                && !pool.getThreadPoolExecutor().getQueue().isEmpty()
                && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
    }

    /** 스레드 max개가 모두 실행 중이고 큐가 가득 찬 상태가 잠깐 뒤에도 그대로인지 본다. */
    private boolean saturated(ThreadPoolTaskExecutor pool) throws InterruptedException {
        for (int check = 0; check < 2; check++) {
            if (pool.getActiveCount() < pool.getMaxPoolSize()
                    || pool.getThreadPoolExecutor().getQueue().remainingCapacity() != 0) {
                return false;
            }
            Thread.sleep(50);
        }
        return true;
    }

    private Map<String, Object> executorState() {
        ThreadPoolTaskExecutor pool = pool();
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("poolSize", pool.getPoolSize());
        s.put("active", pool.getActiveCount());
        s.put("queued", pool.getThreadPoolExecutor().getQueue().size());
        s.put("queueRemaining", pool.getThreadPoolExecutor().getQueue().remainingCapacity());
        return s;
    }

    private void awaitExecutorIdle() throws InterruptedException {
        ThreadPoolTaskExecutor pool = pool();
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(2);
        while (pool.getActiveCount() > 0 || !pool.getThreadPoolExecutor().getQueue().isEmpty()) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("mailExecutor did not drain: " + executorState());
            }
            Thread.sleep(20);
        }
    }

    private ThreadPoolTaskExecutor pool() {
        return (ThreadPoolTaskExecutor) mailExecutor;
    }

    private int sendResultMail(ClubFixture club) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + port + "/api/admin/applies/" + club.clubId() + "/send-email?kind=DOCUMENT"))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .header("Authorization", club.token())
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"pass\":{\"title\":\"합격\",\"body\":\"축하합니다\"},\"fail\":{\"title\":\"불합격\",\"body\":\"다음에 만나요\"}}"))
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

    private String postgresVersion() {
        try (Connection c = DriverManager.getConnection(dataSource.getJdbcUrl(), "postgres", "");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("show server_version")) {
            rs.next();
            return rs.getString(1);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** 커넥션을 반납한 요청 스레드의 URI로 결과 메일 요청과 공지 조회의 점유 시간을 나눈다. */
    private static void recordUsage(long millis) {
        int source = OTHER;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            source = attrs.getRequest().getRequestURI().endsWith("/send-email") ? SEND : PROBE;
        }
        USAGE.add(new long[]{millis, source});
    }

    /** source가 -1이면 전체. */
    private static List<Long> usageOf(List<long[]> usage, int source) {
        return usage.stream().filter(u -> source < 0 || u[1] == source).map(u -> u[0]).toList();
    }

    private static void resetPoolMetrics() {
        USAGE.clear();
        ACQUIRE_NANOS.clear();
        TIMEOUTS.set(0);
    }

    private static String env(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
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
        private final AtomicReference<Exception> failure = new AtomicReference<>();
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
                    failure.set(e);
                }
            });
            thread.start();
        }

        /** 샘플러 스레드가 실패했으면 빈 표본으로 결과를 남기지 않도록 측정을 실패시킨다. */
        void stop() throws InterruptedException {
            running.set(false);
            thread.join();
            if (failure.get() != null) {
                throw new IllegalStateException("pool/pg sampler failed", failure.get());
            }
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
