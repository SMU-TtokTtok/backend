package org.project.ttokttok.infrastructure.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 이메일 인증코드 확인 시도 횟수를 센다.
 *
 * 이메일마다 해시 키 하나에 클라이언트 IP별 횟수와 전체 횟수를 필드로 둔다.
 * HINCRBY 로 세므로 동시에 들어온 요청도 각자 다른 횟수를 받는다.
 * 새 코드를 발송하면 키를 지워 두 횟수를 함께 초기화하고, 그 밖에는 TTL 이 지나면 사라진다.
 */
@Service
@RequiredArgsConstructor
public class VerificationAttemptRedisService {

    private static final String ATTEMPT_KEY = "email-verification:attempts:";
    private static final String TOTAL_FIELD = "total";
    private static final String IP_FIELD = "ip:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 이 IP 의 시도 횟수를 1 올리고 올린 값을 돌려준다.
     *
     * @param ttl 마지막 시도부터 횟수를 유지할 시간
     */
    public long incrementForIp(String email, String clientIp, Duration ttl) {
        return increment(email, IP_FIELD + clientIp, ttl);
    }

    /**
     * 이메일 전체 시도 횟수를 1 올리고 올린 값을 돌려준다.
     *
     * @param ttl 마지막 시도부터 횟수를 유지할 시간
     */
    public long incrementTotal(String email, Duration ttl) {
        return increment(email, TOTAL_FIELD, ttl);
    }

    public void reset(String email) {
        redisTemplate.delete(ATTEMPT_KEY + email);
    }

    private long increment(String email, String field, Duration ttl) {
        String key = ATTEMPT_KEY + email;
        Long attempts = redisTemplate.opsForHash().increment(key, field, 1);
        redisTemplate.expire(key, ttl);
        return attempts == null ? Long.MAX_VALUE : attempts;
    }
}
