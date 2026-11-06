package org.project.ttokttok.global.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.jwt.JwtFactory;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;

/**
 * #417 measures what production-level (INFO) logging emits per request on a real Tomcat.
 * Runs only when TTOKTTOK_LOG_EXPERIMENT_OUT is set; the same class measures before and after.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "logging.level.org.project.ttokttok=INFO",
        "logging.level.org.springframework.mail=INFO",
        "logging.level.org.springframework.jdbc.core=INFO",
        "logging.level.org.hibernate.SQL=OFF"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_LOG_EXPERIMENT_OUT", matches = ".+")
class LoggingVolumeExperimentTest {

    private static final int REQUESTS = 200;
    private static final int RECIPIENTS = 100;
    private static final int MONITOR_LINES_PER_SECOND = 200;
    private static final String EMAIL = "member@sangmyung.kr";
    private static final String PUBLIC_PATH = "/api/clubs/popular";
    private static final String PROTECTED_PATH = "/api/favorites";
    private static final Pattern RAW_EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@sangmyung\\.kr");
    private static final Pattern MAIL_SUMMARY = Pattern.compile("성공: [0-9]+건");
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();

    @LocalServerPort private int port;
    @Autowired private JwtFactory jwtFactory;
    @Autowired private RefreshTokenRedisService refreshTokenRedisService;
    @Autowired private EmailService emailService;
    @Autowired private UserRepository userRepository;
    @MockitoSpyBean private RedisTemplate<String, String> redisTemplate;
    @MockitoBean private JavaMailSender mailSender;
    @MockitoBean private S3Service s3Service;
    @MockitoBean private FCMService fcmService;

    private final HttpClient client = HttpClient.newHttpClient();
    private ListAppender<ILoggingEvent> appender;
    private PatternLayout layout;

    @BeforeEach
    void capture() {
        doReturn(false).when(redisTemplate).hasKey(anyString());
        doReturn(Mockito.mock(ValueOperations.class)).when(redisTemplate).opsForValue();
        given(mailSender.createMimeMessage()).willAnswer(i -> new MimeMessage((Session) null));
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p 1 --- [%t] %logger{40} : %m%n%ex");
        layout.start();
        appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
    }

    @AfterEach
    void release() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(appender);
    }

    @AfterAll
    static void write() throws Exception {
        Path path = Path.of(System.getenv("TTOKTTOK_LOG_EXPERIMENT_OUT"));
        Files.createDirectories(path.toAbsolutePath().getParent());
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(path.toFile(), RESULTS);
    }

    @Test
    void measure() throws Exception {
        userRepository.save(User.signUp(EMAIL, "encoded", "member", true));
        String valid = jwtFactory.generateValidToken(EMAIL, Role.ROLE_USER);
        String expired = jwtFactory.generateExpiredToken(EMAIL, Role.ROLE_USER);
        String wrongSignature = Jwts.builder().setSubject(EMAIL).setIssuer("x")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256)).compact();

        send(PUBLIC_PATH, "Bearer " + valid); // warm up lazily initialised beans outside the measured window
        send(PROTECTED_PATH, "Bearer " + valid);

        http("public_expired_token", PUBLIC_PATH, "Bearer " + expired, expired);
        http("public_malformed_token", PUBLIC_PATH, "Bearer not.a.jwt", "not.a.jwt");
        http("public_wrong_signature", PUBLIC_PATH, "Bearer " + wrongSignature, wrongSignature);
        http("public_lowercase_bearer", PUBLIC_PATH, "bearer " + valid, valid);
        http("public_anonymous", PUBLIC_PATH, null, "no-token");
        http("public_valid_token", PUBLIC_PATH, "Bearer " + valid, valid);
        http("protected_expired_token", PROTECTED_PATH, "Bearer " + expired, expired);
        http("protected_malformed_token", PROTECTED_PATH, "Bearer not.a.jwt", "not.a.jwt");
        http("protected_wrong_signature", PROTECTED_PATH, "Bearer " + wrongSignature, wrongSignature);
        http("protected_valid_token", PROTECTED_PATH, "Bearer " + valid, valid);
        logout(valid);
        resultMail("result_mail_success", false);
        resultMail("result_mail_smtp_outage", true);
    }

    private int send(String path, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private void http(String name, String path, String authorization, String secret) throws Exception {
        appender.list.clear();
        Map<String, Integer> statuses = new TreeMap<>();
        for (int i = 0; i < REQUESTS; i++) {
            statuses.merge(String.valueOf(send(path, authorization)), 1, Integer::sum);
        }
        record(name, REQUESTS, statuses, secret);
    }

    private void logout(String token) {
        appender.list.clear();
        IntStream.range(0, REQUESTS).forEach(i -> refreshTokenRedisService.addAccessTokenToBlacklist(token, 60_000));
        record("logout_blacklist", REQUESTS, Map.of(), token);
    }

    private void resultMail(String name, boolean outage) throws Exception {
        appender.list.clear();
        if (outage) {
            willThrow(new MailSendException("smtp unavailable")).given(mailSender).send(any(MimeMessage.class));
        }
        List<String> recipients = IntStream.range(0, RECIPIENTS).mapToObj(i -> "user" + i + "@sangmyung.kr").toList();
        emailService.sendResultMail(recipients, new ResultMailContent("title", "body"));
        awaitMailSummary();
        record(name, RECIPIENTS, Map.of(), "no-token");
    }

    private void awaitMailSummary() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (snapshot().stream().anyMatch(e -> MAIL_SUMMARY.matcher(e.getFormattedMessage()).find())) {
                return;
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Result mail summary was not logged");
    }

    private List<ILoggingEvent> snapshot() {
        return new ArrayList<>(appender.list);
    }

    private void record(String name, int requests, Map<String, Integer> statuses, String secret) {
        List<ILoggingEvent> events = snapshot();
        long lines = 0;
        long bytes = 0;
        long stacks = 0;
        long tokenLeaks = 0;
        long emailLeaks = 0;
        Map<String, Integer> levels = new TreeMap<>();
        for (ILoggingEvent event : events) {
            String text = layout.doLayout(event);
            lines += text.lines().count();
            bytes += text.getBytes(StandardCharsets.UTF_8).length;
            stacks += event.getThrowableProxy() == null ? 0 : 1;
            tokenLeaks += text.contains(secret) ? 1 : 0;
            emailLeaks += RAW_EMAIL.matcher(text).find() ? 1 : 0;
            levels.merge(event.getLevel().toString(), 1, Integer::sum);
        }
        double perRequest = (double) lines / requests;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("requests", requests);
        row.put("statuses", statuses);
        row.put("events", events.size());
        row.put("levels", levels);
        row.put("lines", lines);
        row.put("bytes", bytes);
        row.put("linesPerRequest", round(perRequest));
        row.put("bytesPerRequest", round((double) bytes / requests));
        row.put("stackTraces", stacks);
        row.put("eventsWithToken", tokenLeaks);
        row.put("eventsWithRawEmail", emailLeaks);
        row.put("requestsPerSecondAtMonitorLimit", perRequest == 0 ? null : round(MONITOR_LINES_PER_SECOND / perRequest));
        RESULTS.put(name, row);
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
