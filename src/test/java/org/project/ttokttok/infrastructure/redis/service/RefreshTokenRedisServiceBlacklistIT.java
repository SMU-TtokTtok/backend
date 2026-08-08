package org.project.ttokttok.infrastructure.redis.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 액세스 토큰 블랙리스트 등록을 <b>실제 Redis</b>에 대해 검증한다.
 *
 * {@link RefreshTokenRedisServiceTest}는 {@code RedisTemplate}을 목으로 대체하므로
 * TTL 0을 넘겨도 통과한다. 실제 Redis는 TTL 0인 SET을 거부하기 때문에
 * 만료된 액세스 토큰으로 로그아웃하면 500이 발생했다. 그 회귀를 막는 테스트다.
 */
@SpringBootTest
class RefreshTokenRedisServiceBlacklistIT {

    @Autowired
    private RefreshTokenRedisService refreshTokenRedisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:access:";
    private static final String REFRESH_KEY_PREFIX = "refresh:";

    private final String accessToken = "it-access-" + UUID.randomUUID();
    private final String refreshToken = "it-refresh-" + UUID.randomUUID();

    @AfterEach
    void tearDown() {
        redisTemplate.delete(BLACKLIST_KEY_PREFIX + accessToken);
        redisTemplate.delete(REFRESH_KEY_PREFIX + refreshToken);
    }

    @Test
    @DisplayName("남은 만료시간이 0이어도 블랙리스트 등록이 예외 없이 끝난다")
    void addAccessTokenToBlacklist_withZeroExpiry_doesNotThrow() {
        assertThatCode(() -> refreshTokenRedisService.addAccessTokenToBlacklist(accessToken, 0L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("남은 만료시간이 음수여도 블랙리스트 등록이 예외 없이 끝난다")
    void addAccessTokenToBlacklist_withNegativeExpiry_doesNotThrow() {
        assertThatCode(() -> refreshTokenRedisService.addAccessTokenToBlacklist(accessToken, -1000L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("남은 만료시간이 양수면 블랙리스트에 등록된다")
    void addAccessTokenToBlacklist_withPositiveExpiry_marksBlacklisted() {
        refreshTokenRedisService.addAccessTokenToBlacklist(accessToken, 60_000L);

        assertThat(refreshTokenRedisService.isAccessTokenBlacklisted(accessToken)).isTrue();
    }

    @Test
    @DisplayName("이미 만료된 액세스 토큰으로 로그아웃해도 예외 없이 끝난다")
    void logout_withExpiredAccessToken_doesNotThrow() {
        assertThatCode(() -> refreshTokenRedisService.logout(refreshToken, accessToken, 0L))
                .doesNotThrowAnyException();
    }
}
