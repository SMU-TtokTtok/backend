package org.project.ttokttok.domain.applicant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.exception.ConstraintViolationException;
import org.project.ttokttok.TtokttokApplication;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.jwt.JwtFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Standalone test-classpath server. No application configuration or credentials are copied into its runtime. */
public class DuplicateApplyBenchmarkServer {
    private static final String CONSTRAINT = "uk_applicants_user_email_applyform";
    private static int expectedGroups;

    public static void main(String[] args) throws Exception {
        String db = System.getenv("TTOKTTOK_CONCURRENCY_JDBC_URL");
        if (!"jdbc:postgresql://ttokttok-426-repro-pg:5432/ttokttok426".equals(db)) {
            throw new IllegalArgumentException("Only the dedicated Docker database is allowed");
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", db);
        properties.put("spring.datasource.username", "postgres");
        properties.put("spring.datasource.password", "");
        properties.put("spring.datasource.hikari.maximum-pool-size", 10);
        properties.put("spring.datasource.hikari.minimum-idle", 10);
        properties.put("spring.datasource.hikari.data-source-properties.ApplicationName", "duplicate426");
        properties.put("spring.jpa.hibernate.ddl-auto", System.getenv().getOrDefault("TTOKTTOK_BENCH_DDL", "create"));
        properties.put("spring.jpa.open-in-view", false);
        properties.put("spring.flyway.enabled", false);
        properties.put("spring.main.allow-bean-definition-overriding", true);
        properties.put("server.port", 8080);
        properties.put("jwt.issuer", "benchmark426");
        properties.put("jwt.secret", System.getenv("TTOKTTOK_BENCH_JWT_SECRET"));
        properties.put("spring.data.redis.host", "127.0.0.1");
        properties.put("spring.data.redis.port", 1);
        properties.put("spring.mail.host", "127.0.0.1");
        properties.put("spring.mail.port", 1);
        properties.put("email.from.address", "benchmark@example.com");
        properties.put("email.from.name", "benchmark");
        properties.put("email.reply-to", "benchmark@example.com");
        properties.put("firebase.json", "not-used");
        properties.put("cloud.aws.credentials.access-key", "benchmark");
        properties.put("cloud.aws.credentials.secret-key", "benchmark");
        properties.put("cloud.aws.region.static", "ap-northeast-2");
        properties.put("cloud.aws.s3.bucket", "benchmark");
        properties.put("cloud.aws.s3.endpoint", "http://127.0.0.1:1");
        properties.put("file-cloud.url", "http://127.0.0.1:1");
        properties.put("server.url", "http://127.0.0.1:8080");
        properties.put("google.oauth.jwk-set-uri", "http://127.0.0.1:1");
        properties.put("google.oauth.client-id", "benchmark");
        properties.put("logging.level.root", "ERROR");
        properties.put("logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper", "OFF");
        SpringApplication app = new SpringApplication(TtokttokApplication.class, Mocks.class);
        app.setDefaultProperties(properties);
        var context = app.run("--spring.config.name=duplicate-apply-benchmark");
        JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
        if (!"V0".equals(System.getenv("TTOKTTOK_BENCH_VARIANT"))
                && "create".equals(properties.get("spring.jpa.hibernate.ddl-auto"))) {
            jdbc.execute("ALTER TABLE applicants ADD CONSTRAINT " + CONSTRAINT + " UNIQUE (user_email, applyform_id)");
        }
        ObjectMapper mapper = context.getBean(ObjectMapper.class);
        var audit = context.getBean(ConflictAudit.class);
        HttpServer control = HttpServer.create(new InetSocketAddress(8081), 0);
        control.createContext("/", exchange -> {
            try {
                Map<String, String> parameters = new HashMap<>();
                String query = exchange.getRequestURI().getRawQuery();
                if (query != null && !query.isBlank()) {
                    for (String pair : query.split("&")) {
                        String[] parts = pair.split("=", 2);
                        parameters.put(parts[0], parts[1]);
                    }
                }
                Object result = switch (exchange.getRequestURI().getPath()) {
                    case "/prepare" -> prepare(context.getBean(EntityManager.class),
                            context.getBean(PlatformTransactionManager.class), jdbc,
                            context.getBean(JwtFactory.class), audit, parameters);
                    case "/metrics" -> metrics(context.getBean(DataSource.class), jdbc, audit);
                    case "/verify" -> verify(jdbc, audit);
                    default -> Map.of("ready", true);
                };
                byte[] body = mapper.writeValueAsBytes(result);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (Exception failure) {
                byte[] body = failure.getClass().getSimpleName().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, body.length);
                exchange.getResponseBody().write(body);
            } finally {
                exchange.close();
            }
        });
        control.setExecutor(Executors.newFixedThreadPool(2));
        control.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> control.stop(0)));
        System.out.println("DUPLICATE_BENCH_READY");
    }

    private static Object prepare(EntityManager em, PlatformTransactionManager manager, JdbcTemplate jdbc,
                                  JwtFactory jwt, ConflictAudit audit, Map<String, String> parameters) {
        String scenario = parameters.get("scenario");
        int vus = Integer.parseInt(parameters.get("vus"));
        int rounds = Integer.parseInt(parameters.get("rounds"));
        if (!Set.of("hot", "users", "forms").contains(scenario) || vus < 1 || vus > 50 || rounds < 1 || rounds > 100) {
            throw new IllegalArgumentException("Invalid benchmark cell");
        }
        // This server admits only its isolated DB; preparation never runs during an HTTP workload.
        jdbc.execute("TRUNCATE TABLE applicants, users, admins, clubs, applyforms, temp_applicants RESTART IDENTITY CASCADE");
        audit.conflicts.set(0);
        List<List<Map<String, String>>> batches = new ArrayList<>();
        new TransactionTemplate(manager).executeWithoutResult(ignored -> {
            List<ApplyForm> forms = new ArrayList<>();
            for (int i = 0; i < (scenario.equals("forms") ? vus : 1); i++) {
                Admin admin = Admin.adminJoin("benchmark426_" + i, "test-password");
                em.persist(admin);
                Club club = Club.builder().admin(admin).clubName("bench-" + i).clubUniv(ClubUniv.ENGINEERING).build();
                em.persist(club);
                ApplyForm form = ApplyForm.createApplyForm(club, false, LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(10), null, null, 100000, Set.of(ApplicableGrade.FIRST_GRADE),
                        "benchmark", "benchmark", List.of(new Question("text", "text", "", QuestionType.SHORT_ANSWER, true, List.of())));
                em.persist(form);
                forms.add(form);
            }
            for (int round = 0; round < rounds; round++) {
                List<Map<String, String>> batch = new ArrayList<>();
                String sharedEmail = "bench" + round + "@sangmyung.kr";
                if (!scenario.equals("users")) {
                    em.persist(User.signUp(sharedEmail, "test-password", "benchmark", true));
                }
                for (int i = 0; i < vus; i++) {
                    String email = scenario.equals("users") ? "bench" + round + "-" + i + "@sangmyung.kr" : sharedEmail;
                    if (scenario.equals("users")) {
                        em.persist(User.signUp(email, "test-password", "benchmark", true));
                    }
                    ApplyForm form = forms.get(scenario.equals("forms") ? i : 0);
                    batch.add(Map.of("clubId", form.getClub().getId(), "formId", form.getId(), "email", email,
                            "token", jwt.generateValidToken(email, Role.ROLE_USER)));
                }
                batches.add(batch);
            }
        });
        expectedGroups = rounds * (scenario.equals("hot") ? 1 : vus);
        return Map.of("batches", batches, "scenario", scenario, "vus", vus, "rounds", rounds);
    }

    private static Object metrics(DataSource dataSource, JdbcTemplate jdbc, ConflictAudit audit) {
        var pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
        Long waiting = jdbc.queryForObject("select count(*) from pg_stat_activity where application_name='duplicate426' and wait_event_type='Lock' and pid<>pg_backend_pid()", Long.class);
        return Map.of("activeConnections", pool.getActiveConnections(), "pendingConnections", pool.getThreadsAwaitingConnection(),
                "lockWaiting", waiting, "uniqueConflicts", audit.conflicts.get());
    }

    private static Object verify(JdbcTemplate jdbc, ConflictAudit audit) {
        long groups = jdbc.queryForObject("select count(*) from (select user_email,applyform_id from applicants group by user_email,applyform_id) g", Long.class);
        return Map.of("applications", jdbc.queryForObject("select count(*) from applicants", Long.class),
                "documents", jdbc.queryForObject("select count(*) from document_phases", Long.class),
                "duplicateGroups", jdbc.queryForObject("select count(*) from (select 1 from applicants group by user_email,applyform_id having count(*)>1) g", Long.class),
                "missingGroups", expectedGroups - groups, "expectedGroups", expectedGroups, "uniqueConflicts", audit.conflicts.get());
    }

    @TestConfiguration
    static class Mocks {
        @Bean static org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor isolatedInfrastructure() {
            return new org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor() {
                public void postProcessBeanDefinitionRegistry(org.springframework.beans.factory.support.BeanDefinitionRegistry registry) {
                    Map<String, Class<?>> mocks = Map.of(
                            "s3Service", org.project.ttokttok.infrastructure.s3.service.S3Service.class,
                            "emailService", org.project.ttokttok.infrastructure.email.service.EmailService.class,
                            "FCMService", org.project.ttokttok.infrastructure.firebase.service.FCMService.class,
                            "refreshTokenRedisService", org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService.class,
                            "firebaseApp", com.google.firebase.FirebaseApp.class,
                            "firebaseMessaging", com.google.firebase.messaging.FirebaseMessaging.class);
                    mocks.forEach((name, type) -> {
                        if (registry.containsBeanDefinition(name)) registry.removeBeanDefinition(name);
                        var definition = new org.springframework.beans.factory.support.RootBeanDefinition(type);
                        definition.setInstanceSupplier(() -> org.mockito.Mockito.mock(type));
                        definition.setDestroyMethodName("");
                        registry.registerBeanDefinition(name, definition);
                    });
                    if (registry.containsBeanDefinition("org.springframework.context.annotation.internalScheduledAnnotationProcessor")) {
                        registry.removeBeanDefinition("org.springframework.context.annotation.internalScheduledAnnotationProcessor");
                    }
                }
                public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory factory) { }
            };
        }
        @Bean ConflictAudit conflictAudit() { return new ConflictAudit(); }
    }

    @Aspect
    static class ConflictAudit {
        final AtomicLong conflicts = new AtomicLong();
        @Around("bean(applicantRepository) && execution(* *(..))")
        Object countConflicts(ProceedingJoinPoint point) throws Throwable {
            try {
                return point.proceed();
            } catch (Throwable failure) {
                for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                    if (cause instanceof ConstraintViolationException violation && CONSTRAINT.equals(violation.getConstraintName())) {
                        conflicts.incrementAndGet();
                        break;
                    }
                }
                throw failure;
            }
        }
    }
}
