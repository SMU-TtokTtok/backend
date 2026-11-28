package org.project.ttokttok.infrastructure.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 이메일 인증코드 확인 시도 횟수를 이메일별로 센다.
 *
 * INCR 로 세므로 동시에 들어온 요청도 각자 다른 횟수를 받는다.
 * 새 코드를 발송하면 횟수를 지우고, 그 밖에는 TTL 이 지나면 사라진다.
 */
@Service
@RequiredArgsConstructor
public class VerificationAttemptRedisService {

    private static final String ATTEMPT_KEY = "email-verification:attempts:";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 시도 횟수를 1 올리고 올린 값을 돌려준다.
     *
     * @param ttl 마지막 시도부터 횟수를 유지할 시간
     */
    public long increment(String email, Duration ttl) {
        String key = ATTEMPT_KEY + email;
        Long attempts = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, ttl);
        return attempts == null ? Long.MAX_VALUE : attempts;
    }

    public void reset(String email) {
        redisTemplate.delete(ATTEMPT_KEY + email);
    }
}
