package org.project.ttokttok.domain.applicant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.exception.AlreadyClubMemberException;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #432 합격 확정 동시성 후보 비교 실험. 전용 로컬 PostgreSQL(ttokttok426)에서만 실행되며
 * 운영 코드의 선택 분기가 아니다. 후보마다 같은 부하를 반복해 중복 부원 수, 오류 분포, 지연을 JSON으로 남긴다.
 * 후보 알고리즘은 이 파일 안에서 실제 Repository/JDBC로 구현하고, V0 기준선은 실제 서비스로도 한 번 확인한다.
 * 환경 변수: TTOKTTOK_CONCURRENCY_JDBC_URL(필수), TTOKTTOK_EXPERIMENT_OUT(결과 JSON 경로),
 * TTOKTTOK_EXPERIMENT_THREADS/REPS/PASSERS(기본 20/20/30).
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=finalize_race_experiment",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.datasource.hikari.connection-init-sql=SET search_path TO finalize_race_experiment",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.maximum-pool-size=60",
        // SQL 로그가 지연 측정을 왜곡하므로 끈다. 교착 상태 오류는 결과 JSON의 DEADLOCK 건수로 집계한다.
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF",
        "logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=OFF"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_CONCURRENCY_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:\\d+/ttokttok426")
class ApplicantFinalizeRaceExperimentTest {

    private static final String UNIQUE_NAME = "uk_club_members_club_email";

    private enum Lock { NONE, ROW, ADVISORY }

    private enum Write { CHECK_SAVE, ON_CONFLICT }

    private enum Order { NATURAL, SHUFFLED, SORTED }

    /** unique: club_members(club_id,email) UNIQUE 존재 여부. */
    private enum Variant {
        V0_CHECK_SAVE(false, Lock.NONE, Write.CHECK_SAVE, Order.NATURAL),
        V1_UNIQUE_SHUFFLED(true, Lock.NONE, Write.CHECK_SAVE, Order.SHUFFLED),
        V1S_UNIQUE_SORTED(true, Lock.NONE, Write.CHECK_SAVE, Order.SORTED),
        V2_UPSERT_SHUFFLED(true, Lock.NONE, Write.ON_CONFLICT, Order.SHUFFLED),
        V2S_UPSERT_SORTED(true, Lock.NONE, Write.ON_CONFLICT, Order.SORTED),
        V3_ROW_LOCK(false, Lock.ROW, Write.CHECK_SAVE, Order.NATURAL),
        V4_ROW_LOCK_UNIQUE(true, Lock.ROW, Write.CHECK_SAVE, Order.NATURAL),
        V5_ADVISORY_LOCK(false, Lock.ADVISORY, Write.CHECK_SAVE, Order.NATURAL);

        final boolean unique;
        final Lock lock;
        final Write write;
        final Order order;

        Variant(boolean unique, Lock lock, Write write, Order order) {
            this.unique = unique;
            this.lock = lock;
            this.write = write;
            this.order = order;
        }
    }

    private record Fixture(String clubId, String formId, List<String> emails) {}

    private record Outcome(String type, double ms) {}

    private record Rep(List<Outcome> outcomes, double wallMs, long members, long expectedMembers, long duplicateGroups) {}

    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ApplicantRepository applicantRepository;
    @Autowired private ClubMemberRepository clubMemberRepository;
    @Autowired private ApplicantAdminService applicantAdminService;
    @MockitoBean private S3Service s3Service;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;

    private final int threads = intEnv("TTOKTTOK_EXPERIMENT_THREADS", 20);
    private final int reps = intEnv("TTOKTTOK_EXPERIMENT_REPS", 20);
    private final int passers = intEnv("TTOKTTOK_EXPERIMENT_PASSERS", 30);

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
    void compareCandidates() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conditions", Map.of("threads", threads, "reps", reps, "passersPerClub", passers,
                "isolation", "READ_COMMITTED(default)", "postgres", jdbcTemplate.queryForObject("select version()", String.class)));

        result.put("hot_same_club", scenario("hot"));
        result.put("distinct_clubs", scenario("distinct"));
        result.put("mixed_finalize_and_manual_add", mixed());
        result.put("v0_real_service", realServiceBaseline());
        result.put("lock_scope", lockScope());

        File out = new File(System.getenv().getOrDefault("TTOKTTOK_EXPERIMENT_OUT", "build/finalize-race-experiment.json"));
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, result);
        System.out.println("FINALIZE_RACE_EXPERIMENT written=" + out.getAbsolutePath());
        assertThat(out).exists();
    }

    // ---- scenarios ----

    private Map<String, Object> scenario(String kind) throws Exception {
        Map<String, Object> byVariant = new LinkedHashMap<>();
        for (Variant variant : Variant.values()) {
            prepare(variant);
            List<Rep> results = new ArrayList<>();
            for (int rep = 0; rep < reps; rep++) {
                if (kind.equals("hot")) {
                    Fixture f = createClub(passers);
                    results.add(runConcurrently(threads, i -> finalizeOnce(variant, f), List.of(f), passers));
                } else {
                    List<Fixture> clubs = new ArrayList<>();
                    for (int i = 0; i < threads; i++) {
                        clubs.add(createClub(passers));
                    }
                    results.add(runConcurrently(threads, i -> finalizeOnce(variant, clubs.get(i)), clubs, passers));
                }
            }
            byVariant.put(variant.name(), summarize(results));
            System.out.println("FINALIZE_RACE_EXPERIMENT scenario=" + kind + " variant=" + variant + " "
                    + byVariant.get(variant.name()));
        }
        return byVariant;
    }

    /** 합격 확정(N/2)과 수동 부원 등록(N/2)이 같은 (동아리, 이메일) 한 건을 두고 경쟁. 잠금은 협조 여부를 나눠 측정. */
    private Map<String, Object> mixed() throws Exception {
        Map<String, Object> byVariant = new LinkedHashMap<>();
        for (Variant variant : new Variant[]{Variant.V0_CHECK_SAVE, Variant.V2S_UPSERT_SORTED,
                Variant.V3_ROW_LOCK, Variant.V4_ROW_LOCK_UNIQUE, Variant.V5_ADVISORY_LOCK}) {
            for (boolean manualCooperates : variant.lock == Lock.NONE ? new boolean[]{false} : new boolean[]{true, false}) {
                prepare(variant);
                List<Rep> results = new ArrayList<>();
                for (int rep = 0; rep < reps * 2; rep++) {
                    Fixture f = createClub(1);
                    results.add(runConcurrently(threads, i -> i % 2 == 0
                            ? finalizeOnce(variant, f)
                            : manualAddOnce(variant, manualCooperates, f), List.of(f), 1));
                }
                String key = variant.name() + (variant.lock == Lock.NONE ? "" : manualCooperates ? "+manual_takes_lock" : "+manual_no_lock");
                byVariant.put(key, summarize(results));
                System.out.println("FINALIZE_RACE_EXPERIMENT scenario=mixed variant=" + key + " " + byVariant.get(key));
            }
        }
        return byVariant;
    }

    /** 현재 운영 코드(실제 서비스)로 같은 부하를 한 번 더 확인해 V0 기준선이 하네스 산물이 아님을 확인. */
    private Map<String, Object> realServiceBaseline() throws Exception {
        prepare(Variant.V0_CHECK_SAVE);
        List<Rep> results = new ArrayList<>();
        Map<String, Integer> responseShapes = new TreeMap<>();
        for (int rep = 0; rep < reps; rep++) {
            Fixture f = createClub(passers);
            String username = jdbcTemplate.queryForObject("""
                    select a.username from admins a join clubs c on c.admin_id = a.id where c.id = ?
                    """, String.class, f.clubId);
            results.add(runConcurrently(threads, i -> {
                long t0 = System.nanoTime();
                try {
                    var response = applicantAdminService.finalizeApplicantsStatus(
                            org.project.ttokttok.domain.applicant.service.dto.request.ApplicantFinalizationRequest
                                    .of(username, f.clubId, "DOCUMENT"));
                    synchronized (responseShapes) {
                        responseShapes.merge(response.toString(), 1, Integer::sum);
                    }
                    return new Outcome("OK", ms(t0));
                } catch (RuntimeException e) {
                    return new Outcome(classify(e), ms(t0));
                }
            }, List.of(f), passers));
        }
        Map<String, Object> summary = summarize(results);
        summary.put("responseShapes", responseShapes);
        System.out.println("FINALIZE_RACE_EXPERIMENT scenario=v0_real_service " + summary);
        return summary;
    }

    /** 동아리 행 잠금이 같은 동아리를 참조하는 다른 쓰기(FK 확인)를 막는지 확인. */
    private Map<String, Object> lockScope() throws Exception {
        prepare(Variant.V3_ROW_LOCK);
        Fixture f = createClub(1);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> holder = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
                entityManager.find(Club.class, f.clubId, LockModeType.PESSIMISTIC_WRITE);
                locked.countDown();
                try {
                    release.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(locked.await(15, TimeUnit.SECONDS)).isTrue();
            Future<Double> insert = executor.submit(() -> {
                long t0 = System.nanoTime();
                new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
                    jdbcTemplate.execute("set local lock_timeout = '5s'");
                    insertMemberRow(f.clubId, "other-" + UUID.randomUUID() + "@sangmyung.kr");
                });
                return ms(t0);
            });
            Map<String, Object> out = new LinkedHashMap<>();
            try {
                out.put("memberInsertWhileClubRowLockedMs", insert.get(8, TimeUnit.SECONDS));
                out.put("blocked", false);
            } catch (Exception e) {
                out.put("blocked", true);
                out.put("error", String.valueOf(e.getCause() != null ? e.getCause().getMessage() : e));
            } finally {
                release.countDown();
                holder.get(15, TimeUnit.SECONDS);
            }
            System.out.println("FINALIZE_RACE_EXPERIMENT scenario=lock_scope " + out);
            return out;
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    // ---- candidate mechanics ----

    private Outcome finalizeOnce(Variant v, Fixture f) {
        long t0 = System.nanoTime();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
                Club club = lockAndLoadClub(v.lock, f.clubId);
                List<Applicant> passed = applicantRepository.findByApplyFormId(f.formId).stream()
                        .filter(a -> a.statusOf(ApplicantPhase.DOCUMENT)
                                .filter(status -> status == PhaseStatus.PASS).isPresent())
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
                switch (v.order) {
                    case SHUFFLED -> Collections.shuffle(passed, new Random());
                    case SORTED -> passed.sort(Comparator.comparing(Applicant::getEmail));
                    default -> { }
                }
                if (v.write == Write.CHECK_SAVE) {
                    List<ClubMember> members = passed.stream()
                            .filter(a -> !clubMemberRepository.existsByClubIdAndEmail(club.getId(), a.getEmail()))
                            .map(a -> toMember(club, a))
                            .toList();
                    clubMemberRepository.saveAllAndFlush(members);
                } else {
                    for (Applicant a : passed) {
                        insertMemberRow(club.getId(), a);
                    }
                }
            });
            return new Outcome("OK", ms(t0));
        } catch (RuntimeException e) {
            return new Outcome(classify(e), ms(t0));
        }
    }

    /** ClubMemberService.createClubMember()와 같은 조회 후 저장 구조. */
    private Outcome manualAddOnce(Variant v, boolean cooperates, Fixture f) {
        long t0 = System.nanoTime();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
                Club club = lockAndLoadClub(cooperates ? v.lock : Lock.NONE, f.clubId);
                String email = f.emails.get(0);
                if (clubMemberRepository.existsByClubIdAndEmail(club.getId(), email)) {
                    throw new AlreadyClubMemberException();
                }
                clubMemberRepository.saveAndFlush(ClubMember.create(club, "수동", MemberRole.MEMBER,
                        Grade.FIRST_GRADE, "컴퓨터공학", email, "010-0000-0000", Gender.MALE));
            });
            return new Outcome("OK", ms(t0));
        } catch (RuntimeException e) {
            return new Outcome(classify(e), ms(t0));
        }
    }

    private Club lockAndLoadClub(Lock lock, String clubId) {
        if (lock == Lock.ADVISORY) {
            jdbcTemplate.queryForList("select pg_advisory_xact_lock(hashtext(?))::text", clubId);
        }
        return lock == Lock.ROW
                ? entityManager.find(Club.class, clubId, LockModeType.PESSIMISTIC_WRITE)
                : entityManager.find(Club.class, clubId);
    }

    private ClubMember toMember(Club club, Applicant a) {
        return ClubMember.create(club, a.getName(), MemberRole.MEMBER, a.getGrade(), a.getMajor(),
                a.getEmail(), a.getPhone(), a.getGender());
    }

    private void insertMemberRow(String clubId, Applicant a) {
        jdbcTemplate.update("""
                insert into club_members (id, club_id, role, grade, major, member_name, email, phone_number, gender,
                                          created_at, updated_at)
                values (?, ?, 'MEMBER', ?, ?, ?, ?, ?, ?, now(), now())
                on conflict (club_id, email) do nothing
                """, UUID.randomUUID().toString(), clubId, a.getGrade().name(), a.getMajor(), a.getName(),
                a.getEmail(), a.getPhone(), a.getGender().name());
    }

    private void insertMemberRow(String clubId, String email) {
        jdbcTemplate.update("""
                insert into club_members (id, club_id, role, grade, major, member_name, email, phone_number, gender,
                                          created_at, updated_at)
                values (?, ?, 'MEMBER', 'FIRST_GRADE', '전공', '이름', ?, '010-0000-0000', 'MALE', now(), now())
                """, UUID.randomUUID().toString(), clubId, email);
    }

    // ---- harness ----

    private void prepare(Variant variant) {
        jdbcTemplate.execute("delete from club_members");
        jdbcTemplate.execute("alter table club_members drop constraint if exists " + UNIQUE_NAME);
        if (variant.unique) {
            jdbcTemplate.execute("alter table club_members add constraint " + UNIQUE_NAME + " unique (club_id, email)");
        }
    }

    private Fixture createClub(int passerCount) {
        Fixture[] holder = new Fixture[1];
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            Admin admin = Admin.adminJoin("race" + UUID.randomUUID().toString().substring(0, 8), "test-password");
            entityManager.persist(admin);
            Club club = Club.builder().admin(admin).clubName("race-" + UUID.randomUUID()).clubUniv(ClubUniv.ENGINEERING).build();
            entityManager.persist(club);
            ApplyForm form = ApplyForm.createApplyForm(club, false,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), null, null, 1000,
                    Set.of(ApplicableGrade.FIRST_GRADE), "지원폼", "설명", List.of(
                            new Question("text", "지원 동기", "설명", QuestionType.SHORT_ANSWER, true, List.of())));
            entityManager.persist(form);
            List<String> emails = new ArrayList<>();
            for (int i = 0; i < passerCount; i++) {
                String email = "p" + i + "-" + UUID.randomUUID().toString().substring(0, 8) + "@sangmyung.kr";
                Applicant applicant = Applicant.createApplicant(email, "합격자" + i, 20, "컴퓨터공학", email,
                        "010-1234-5678", StudentStatus.ENROLLED, Grade.FIRST_GRADE, Gender.MALE, form);
                applicant.submitDocument(List.of());
                applicant.passDocumentEvaluation();
                entityManager.persist(applicant);
                emails.add(email);
            }
            holder[0] = new Fixture(club.getId(), form.getId(), emails);
        });
        return holder[0];
    }

    private Rep runConcurrently(int count, java.util.function.IntFunction<Outcome> task, List<Fixture> clubs,
                                int expectedPerClub) throws Exception {
        var executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("start timed out");
                    }
                    return task.apply(index);
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            long t0 = System.nanoTime();
            start.countDown();
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(90, TimeUnit.SECONDS));
            }
            double wall = ms(t0);
            long members = 0;
            long duplicates = 0;
            for (Fixture f : clubs) {
                members += jdbcTemplate.queryForObject("select count(*) from club_members where club_id = ?", Long.class, f.clubId);
                duplicates += jdbcTemplate.queryForObject("""
                        select count(*) from (select 1 from club_members where club_id = ?
                        group by email having count(*) > 1) d
                        """, Long.class, f.clubId);
            }
            return new Rep(outcomes, wall, members, (long) clubs.size() * expectedPerClub, duplicates);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Map<String, Object> summarize(List<Rep> results) {
        Map<String, Integer> outcomeCounts = new TreeMap<>();
        List<Double> okLatencies = new ArrayList<>();
        List<Double> allLatencies = new ArrayList<>();
        List<Double> walls = new ArrayList<>();
        int repsWithDuplicates = 0;
        int repsWrongMemberCount = 0;
        long duplicateGroups = 0;
        for (Rep rep : results) {
            walls.add(rep.wallMs);
            duplicateGroups += rep.duplicateGroups;
            if (rep.duplicateGroups > 0) {
                repsWithDuplicates++;
            }
            if (rep.members != rep.expectedMembers) {
                repsWrongMemberCount++;
            }
            for (Outcome outcome : rep.outcomes) {
                outcomeCounts.merge(outcome.type, 1, Integer::sum);
                allLatencies.add(outcome.ms);
                if (outcome.type.equals("OK")) {
                    okLatencies.add(outcome.ms);
                }
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("reps", results.size());
        summary.put("requests", allLatencies.size());
        summary.put("outcomes", outcomeCounts);
        summary.put("repsWithDuplicateMembers", repsWithDuplicates);
        summary.put("duplicateGroups", duplicateGroups);
        summary.put("repsWithWrongMemberCount", repsWrongMemberCount);
        summary.put("okLatencyMs", percentiles(okLatencies));
        summary.put("allLatencyMs", percentiles(allLatencies));
        summary.put("repWallMs", percentiles(walls));
        return summary;
    }

    private static Map<String, Double> percentiles(List<Double> values) {
        Map<String, Double> p = new LinkedHashMap<>();
        if (values.isEmpty()) {
            return p;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        p.put("p50", round(sorted.get((int) Math.floor((sorted.size() - 1) * 0.50))));
        p.put("p95", round(sorted.get((int) Math.floor((sorted.size() - 1) * 0.95))));
        p.put("max", round(sorted.get(sorted.size() - 1)));
        return p;
    }

    private static double ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static int intEnv(String name, int fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static String classify(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof AlreadyClubMemberException) {
                return "ALREADY_MEMBER_409";
            }
            if (cause instanceof SQLException sql && sql.getSQLState() != null) {
                switch (sql.getSQLState()) {
                    case "23505": return "UNIQUE_CONFLICT";
                    case "40P01": return "DEADLOCK";
                    case "40001": return "SERIALIZATION";
                    default: return "SQL_" + sql.getSQLState();
                }
            }
        }
        return error.getClass().getSimpleName();
    }
}
