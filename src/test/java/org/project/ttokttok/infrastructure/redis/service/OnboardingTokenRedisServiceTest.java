package org.project.ttokttok.infrastructure.redis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OnboardingTokenRedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OnboardingTokenRedisService onboardingTokenRedisService;

    @BeforeEach
    void setUp() {
        onboardingTokenRedisService = new OnboardingTokenRedisService(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("최초 사용 시 true를 반환한다 (SETNX 선점 성공)")
    void markUsed_firstTime_returnsTrue() {
        // given
        given(valueOperations.setIfAbsent(eq("onboarding:used:jti-1"), eq("used"), any(Duration.class)))
                .willReturn(true);

        // when
        boolean result = onboardingTokenRedisService.markUsed("jti-1");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이미 사용된 jti면 false를 반환한다 (재생/동시 요청 차단)")
    void markUsed_alreadyUsed_returnsFalse() {
        // given
        given(valueOperations.setIfAbsent(eq("onboarding:used:jti-1"), eq("used"), any(Duration.class)))
                .willReturn(false);

        // when
        boolean result = onboardingTokenRedisService.markUsed("jti-1");

        // then
        assertThat(result).isFalse();
    }
}
