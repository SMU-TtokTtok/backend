package org.project.ttokttok.infrastructure.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static org.project.ttokttok.global.auth.jwt.service.OnboardingTokenProvider.ONBOARDING_TOKEN_EXPIRY_TIME;

/**
 * 온보딩 토큰의 일회성 사용을 보장하는 Redis 서비스
 *
 * SETNX 로 jti 를 선점하여 재생(replay)과 동시 요청(두 탭) 경쟁을 원자적으로 판별한다.
 * TTL 은 온보딩 토큰 수명과 동일하게 설정하여 토큰 만료 후 키가 자동 정리된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingTokenRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // 온보딩 토큰 사용 여부 키 접두사
    private static final String ONBOARDING_USED_KEY = "onboarding:used:";

    /**
     * 온보딩 토큰(jti)을 사용 처리한다.
     *
     * @param jti 온보딩 토큰의 jti 클레임
     * @return 최초 사용이면 true, 이미 사용된 토큰이면 false
     */
    public boolean markUsed(String jti) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                ONBOARDING_USED_KEY + jti,
                "used",
                Duration.ofMillis(ONBOARDING_TOKEN_EXPIRY_TIME));

        return Boolean.TRUE.equals(acquired);
    }
}
