package org.project.ttokttok.global.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.ServletWebRequest;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/** #417 pins what production-level logging may emit for auth failures, tokens and mail. */
class SensitiveLoggingRegressionTest {

    private static final String URI = "/api/clubs/popular";

    private final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final List<Logger> loggers = new ArrayList<>();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private TokenProvider tokenProvider;
    private AuthUserInfoResolver resolver;

    @BeforeEach
    void setUp() {
        appender.start();
        String secret = Base64.getEncoder().encodeToString(key.getEncoded());
        tokenProvider = new TokenProvider(secret, mock(RefreshTokenRedisService.class));
        ReflectionTestUtils.setField(tokenProvider, "issuer", "ttokttok");
        resolver = new AuthUserInfoResolver(tokenProvider);
    }

    @AfterEach
    void tearDown() {
        loggers.forEach(logger -> logger.detachAppender(appender));
    }

    private void capture(Class<?> type) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        logger.addAppender(appender);
        loggers.add(logger);
    }

    private List<ILoggingEvent> productionEvents() {
        return appender.list.stream().filter(e -> e.getLevel().isGreaterOrEqual(Level.INFO)).toList();
    }

    private String token(SecretKey signingKey, long expiresInMillis) {
        return Jwts.builder().setSubject("member@sangmyung.kr").setIssuer("ttokttok")
                .setExpiration(new Date(System.currentTimeMillis() + expiresInMillis))
                .signWith(signingKey).compact();
    }

    private Object resolve(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", URI);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return resolver.resolveArgument(null, null, new ServletWebRequest(request), null);
    }

    @Test
    void expiredTokenLeavesNoProductionLog() throws Exception {
        capture(TokenProvider.class);
        capture(AuthUserInfoResolver.class);
        String expired = token(key, -60_000);

        assertThat(tokenProvider.validateToken(expired)).isFalse();
        assertThat(resolve("Bearer " + expired)).isNull();

        assertThat(productionEvents()).isEmpty();
    }

    @Test
    void forgedTokenOnProtectedPathIsOneWarnWithoutStackTrace() {
        capture(TokenProvider.class);
        String forged = token(Keys.secretKeyFor(SignatureAlgorithm.HS256), 60_000);

        assertThat(tokenProvider.validateToken(forged)).isFalse();

        assertThat(productionEvents()).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getThrowableProxy()).isNull();
            assertThat(event.getFormattedMessage()).doesNotContain(forged);
        });
    }

    @Test
    void forgedTokenOnPublicPathKeepsUriWithoutTokenOrStackTrace() throws Exception {
        capture(AuthUserInfoResolver.class);
        String forged = token(Keys.secretKeyFor(SignatureAlgorithm.HS256), 60_000);

        assertThat(resolve("Bearer " + forged)).isNull();

        assertThat(productionEvents()).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getThrowableProxy()).isNull();
            assertThat(event.getFormattedMessage()).contains(URI).doesNotContain(forged);
        });
    }

    @Test
    void nonBearerHeaderValueIsNeverLogged() throws Exception {
        capture(AuthUserInfoResolver.class);

        assertThat(resolve("bearer secret-value")).isNull();
        assertThat(resolve(null)).isNull();

        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains("secret-value"));
        assertThat(productionEvents()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void blacklistingNeverLogsTheToken() {
        capture(RefreshTokenRedisService.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        given(redisTemplate.opsForValue()).willReturn(mock(ValueOperations.class));

        new RefreshTokenRedisService(redisTemplate).addAccessTokenToBlacklist("access-token-value", 60_000);

        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains("access-token-value"));
    }

    @Test
    void mailOutageHasMaskedRecipientsAndNoStackTracePerRecipient() {
        capture(EmailService.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        given(mailSender.createMimeMessage()).willAnswer(i -> new MimeMessage((Session) null));
        willThrow(new MailSendException("smtp unavailable")).given(mailSender).send(any(MimeMessage.class));
        EmailService emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@sangmyung.kr");
        ReflectionTestUtils.setField(emailService, "fromName", "ttokttok");
        ReflectionTestUtils.setField(emailService, "replyTo", "noreply@sangmyung.kr");

        ResultMailContent content = new ResultMailContent("title", "body");
        for (String recipient : List.of("kim@sangmyung.kr", "lee@sangmyung.kr")) {
            assertThatThrownBy(() -> emailService.sendResultEmail(recipient, content)).isInstanceOf(RuntimeException.class);
        }

        assertThat(appender.list).hasSize(2);
        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().matches(".*(kim|lee)@sangmyung.*"));
        assertThat(appender.list).allMatch(e -> e.getLevel() == Level.WARN && e.getThrowableProxy() == null);
    }
}
