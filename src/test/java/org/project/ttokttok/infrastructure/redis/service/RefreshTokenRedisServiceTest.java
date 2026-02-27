package org.project.ttokttok.infrastructure.redis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.global.auth.jwt.exception.RefreshTokenExpiredException;
import org.project.ttokttok.global.auth.jwt.exception.RefreshTokenNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenRedisService service;

    private static final String TEST_USERNAME = "test@example.com";
    private static final String TEST_REFRESH_TOKEN = "validRefreshToken123";
    private static final String TEST_ACCESS_TOKEN = "validAccessToken456";
    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:access:";

    @Nested
    @DisplayName("save 메서드는")
    class Describe_save {

        @Test
        @DisplayName("유효한 username과 refreshToken이 주어지면 Redis에 저장한다")
        void saveRefreshToken_withValidInput_savesToRedis() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            service.save(TEST_USERNAME, TEST_REFRESH_TOKEN);

            // then
            verify(redisTemplate).opsForValue();
            verify(valueOperations).set(
                    REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN,
                    TEST_USERNAME,
                    Duration.ofMillis(604800000L)  // 7일 = 604800000ms
            );
        }
    }

    @Nested
    @DisplayName("getUsernameFromRefreshToken 메서드는")
    class Describe_getUsernameFromRefreshToken {

        @Test
        @DisplayName("유효한 refreshToken이 주어지면 username을 반환한다")
        void getUsernameFromRefreshToken_withValidToken_returnsUsername() {
            // given
            when(redisTemplate.hasKey(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN)).thenReturn(true);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN)).thenReturn(TEST_USERNAME);

            // when
            String result = service.getUsernameFromRefreshToken(TEST_REFRESH_TOKEN);

            // then
            assertThat(result).isEqualTo(TEST_USERNAME);
            verify(redisTemplate).hasKey(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN);
            verify(valueOperations).get(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("존재하지 않는 refreshToken이 주어지면 RefreshTokenNotFoundException을 던진다")
        void getUsernameFromRefreshToken_withInvalidToken_throwsException() {
            // given
            when(redisTemplate.hasKey(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> service.getUsernameFromRefreshToken(TEST_REFRESH_TOKEN))
                    .isInstanceOf(RefreshTokenNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteRefreshToken 메서드는")
    class Describe_deleteRefreshToken {

        @Test
        @DisplayName("존재하는 refreshToken이 주어지면 삭제한다")
        void deleteRefreshToken_withExistingToken_deletesToken() {
            // given
            when(redisTemplate.hasKey(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN)).thenReturn(true);

            // when
            service.deleteRefreshToken(TEST_REFRESH_TOKEN);

            // then
            verify(redisTemplate).delete(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("존재하지 않는 refreshToken이 주어지면 아무 동작도 하지 않는다")
        void deleteRefreshToken_withNonExistingToken_doesNothing() {
            // given
            when(redisTemplate.hasKey(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN)).thenReturn(false);

            // when
            service.deleteRefreshToken(TEST_REFRESH_TOKEN);

            // then
            verify(redisTemplate, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("addAccessTokenToBlacklist 메서드는")
    class Describe_addAccessTokenToBlacklist {

        @Test
        @DisplayName("액세스 토큰과 만료시간이 주어지면 블랙리스트에 추가한다")
        void addAccessTokenToBlacklist_withValidInput_addsToBlacklist() {
            // given
            long expiryTime = 3600000L; // 1시간
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            service.addAccessTokenToBlacklist(TEST_ACCESS_TOKEN, expiryTime);

            // then
            verify(valueOperations).set(
                    BLACKLIST_KEY_PREFIX + TEST_ACCESS_TOKEN,
                    "blacklisted",
                    Duration.ofMillis(expiryTime)
            );
        }

        @ParameterizedTest
        @DisplayName("다양한 만료시간이 주어져도 블랙리스트에 추가한다")
        @ValueSource(longs = {1000L, 60000L, 3600000L, 0L})
        void addAccessTokenToBlacklist_withVariousExpiryTimes_addsToBlacklist(long expiryTime) {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            service.addAccessTokenToBlacklist(TEST_ACCESS_TOKEN, expiryTime);

            // then
            verify(valueOperations).set(
                    BLACKLIST_KEY_PREFIX + TEST_ACCESS_TOKEN,
                    "blacklisted",
                    Duration.ofMillis(expiryTime)
            );
        }
    }

    @Nested
    @DisplayName("isAccessTokenBlacklisted 메서드는")
    class Describe_isAccessTokenBlacklisted {

        @Test
        @DisplayName("블랙리스트에 존재하는 토큰이면 true를 반환한다")
        void isAccessTokenBlacklisted_withBlacklistedToken_returnsTrue() {
            // given
            when(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + TEST_ACCESS_TOKEN)).thenReturn(true);

            // when
            boolean result = service.isAccessTokenBlacklisted(TEST_ACCESS_TOKEN);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("블랙리스트에 존재하지 않는 토큰이면 false를 반환한다")
        void isAccessTokenBlacklisted_withNonBlacklistedToken_returnsFalse() {
            // given
            when(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + TEST_ACCESS_TOKEN)).thenReturn(false);

            // when
            boolean result = service.isAccessTokenBlacklisted(TEST_ACCESS_TOKEN);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("hasKey가 null을 반환하면 false를 반환한다")
        void isAccessTokenBlacklisted_withNullHasKeyResult_returnsFalse() {
            // given
            when(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + TEST_ACCESS_TOKEN)).thenReturn(null);

            // when
            boolean result = service.isAccessTokenBlacklisted(TEST_ACCESS_TOKEN);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("logout 메서드는")
    class Describe_logout {

        @Test
        @DisplayName("유효한 토큰들이 주어지면 리프레시 토큰 삭제 및 액세스 토큰 블랙리스트 추가를 수행한다")
        void logout_withValidTokens_deletesRefreshAndBlacklistsAccess() {
            // given
            long accessTokenExpiryTime = 3600000L;
            when(redisTemplate.hasKey(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN)).thenReturn(true);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            service.logout(TEST_REFRESH_TOKEN, TEST_ACCESS_TOKEN, accessTokenExpiryTime);

            // then
            verify(redisTemplate).delete(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN);
            verify(valueOperations).set(
                    BLACKLIST_KEY_PREFIX + TEST_ACCESS_TOKEN,
                    "blacklisted",
                    Duration.ofMillis(accessTokenExpiryTime)
            );
        }

        @Test
        @DisplayName("accessToken이 null이면 리프레시 토큰만 삭제한다")
        void logout_withNullAccessToken_onlyDeletesRefreshToken() {
            // given
            when(redisTemplate.hasKey(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN)).thenReturn(true);

            // when
            service.logout(TEST_REFRESH_TOKEN, null, 0L);

            // then
            verify(redisTemplate).delete(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN);
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰이면 액세스 토큰만 블랙리스트에 추가한다")
        void logout_withNonExistingRefreshToken_onlyBlacklistsAccessToken() {
            // given
            long accessTokenExpiryTime = 3600000L;
            when(redisTemplate.hasKey(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN)).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            service.logout(TEST_REFRESH_TOKEN, TEST_ACCESS_TOKEN, accessTokenExpiryTime);

            // then
            verify(redisTemplate, never()).delete(anyString());
            verify(valueOperations).set(
                    BLACKLIST_KEY_PREFIX + TEST_ACCESS_TOKEN,
                    "blacklisted",
                    Duration.ofMillis(accessTokenExpiryTime)
            );
        }
    }

    @Nested
    @DisplayName("getRefreshTTL 메서드는")
    class Describe_getRefreshTTL {

        private static final long TTL_KEY_NOT_EXIST = -2L;
        private static final long TTL_NO_EXPIRY = -1L;

        @Test
        @DisplayName("유효한 refreshToken이 주어지면 남은 TTL을 반환한다")
        void getRefreshTTL_withValidToken_returnsTTL() {
            // given
            long expectedTTL = 3600000L;
            when(redisTemplate.getExpire(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN, TimeUnit.MILLISECONDS))
                    .thenReturn(expectedTTL);

            // when
            Long result = service.getRefreshTTL(TEST_REFRESH_TOKEN);

            // then
            assertThat(result).isEqualTo(expectedTTL);
        }

        @Test
        @DisplayName("TTL이 null이면 RefreshTokenExpiredException을 던진다")
        void getRefreshTTL_withNullTTL_throwsException() {
            // given
            when(redisTemplate.getExpire(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN, TimeUnit.MILLISECONDS))
                    .thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getRefreshTTL(TEST_REFRESH_TOKEN))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @ParameterizedTest
        @DisplayName("다양한 TTL 값에 대해 올바르게 반환한다")
        @ValueSource(longs = {1L, 1000L, 604800000L})
        void getRefreshTTL_withVariousTTLValues_returnsTTL(long ttl) {
            // given
            when(redisTemplate.getExpire(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN, TimeUnit.MILLISECONDS))
                    .thenReturn(ttl);

            // when
            Long result = service.getRefreshTTL(TEST_REFRESH_TOKEN);

            // then
            assertThat(result).isEqualTo(ttl);
        }

        @Test
        @DisplayName("TTL이 -2(키 없음)이면 RefreshTokenExpiredException을 던진다")
        void getRefreshTTL_withKeyNotExist_throwsException() {
            // given
            when(redisTemplate.getExpire(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN, TimeUnit.MILLISECONDS))
                    .thenReturn(TTL_KEY_NOT_EXIST);

            // when & then
            assertThatThrownBy(() -> service.getRefreshTTL(TEST_REFRESH_TOKEN))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @Test
        @DisplayName("TTL이 -1(TTL 없음, 영구 키)이면 RefreshTokenExpiredException을 던진다")
        void getRefreshTTL_withNoExpiry_throwsException() {
            // given
            when(redisTemplate.getExpire(REFRESH_KEY_PREFIX + TEST_REFRESH_TOKEN, TimeUnit.MILLISECONDS))
                    .thenReturn(TTL_NO_EXPIRY);

            // when & then
            assertThatThrownBy(() -> service.getRefreshTTL(TEST_REFRESH_TOKEN))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }
    }
}