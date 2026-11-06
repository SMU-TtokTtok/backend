package org.project.ttokttok.global.schema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #438 restores the production schema dump, runs pending Flyway migrations and starts the app with
 * ddl-auto=validate, so a broken migration or an entity mismatch fails here instead of during a deploy.
 *
 * <p>The dump stays outside the repository. Run against the dedicated local container only:
 * TTOKTTOK_CONCURRENCY_JDBC_URL, TTOKTTOK_SCHEMA_BASELINE_FILE and TTOKTTOK_SCHEMA_BASELINE_VERSION
 * (the Flyway version the dump was taken at). See deploy/migrations/pending/README.md.
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=true"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_SCHEMA_BASELINE_FILE", matches = ".+")
class SchemaBaselineValidationTest {

    private static final String LOCAL_URL = "jdbc:postgresql://127\\.0\\.0\\.1:\\d+/ttokttok426";
    private static final String BASELINE_DATABASE = "ttokttok_schema_baseline";

    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private S3Service s3Service;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;

    @DynamicPropertySource
    static void restoreBaseline(DynamicPropertyRegistry registry) throws SQLException, IOException {
        String url = System.getenv("TTOKTTOK_CONCURRENCY_JDBC_URL");
        if (url == null || !url.matches(LOCAL_URL)) {
            throw new IllegalArgumentException("Use the dedicated local ttokttok426 test database only");
        }
        String baselineUrl = url.substring(0, url.lastIndexOf('/') + 1) + BASELINE_DATABASE;
        recreateDatabase(url);
        restoreDump(baselineUrl, Path.of(System.getenv("TTOKTTOK_SCHEMA_BASELINE_FILE")));

        registry.add("spring.datasource.url", () -> baselineUrl);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.baseline-version", () -> System.getenv("TTOKTTOK_SCHEMA_BASELINE_VERSION"));
    }

    private static void recreateDatabase(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "postgres", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + BASELINE_DATABASE);
            statement.execute("CREATE DATABASE " + BASELINE_DATABASE);
        }
    }

    // The dump keeps an empty history table; dropping it lets Flyway baseline at the dump's version.
    private static void restoreDump(String url, Path dump) throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection(url, "postgres", "");
             Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(dump));
            statement.execute("DROP TABLE public.flyway_schema_history");
        }
    }

    @Test
    void migrationsApplyAndEntitiesMatchTheProductionSchema() {
        // Reaching this line means Flyway migrated and Hibernate validated every entity at startup.
        Integer failed = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where not success", Integer.class);
        String baseline = jdbcTemplate.queryForObject(
                "select version from flyway_schema_history where type = 'BASELINE'", String.class);

        assertThat(failed).isZero();
        assertThat(baseline).isEqualTo(System.getenv("TTOKTTOK_SCHEMA_BASELINE_VERSION"));
    }
}
