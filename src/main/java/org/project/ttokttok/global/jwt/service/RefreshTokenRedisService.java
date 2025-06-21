package org.project.ttokttok.global.jwt.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.global.jwt.TokenExpiry;
import org.project.ttokttok.global.jwt.exception.RefreshTokenAlreadyExistsException;
import org.project.ttokttok.global.jwt.exception.RefreshTokenNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static org.project.ttokttok.global.jwt.TokenExpiry.REFRESH_TOKEN_EXPIRY_TIME;

@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_KEY = "refresh:";

    public void save(String id, String refreshToken) {
        if (isExistKey(id)) { // 리프레시 토큰이 존재하면 예외 발생
            throw new RefreshTokenAlreadyExistsException();
        }

        redisTemplate.opsForValue().set(
                REFRESH_KEY + id,
                refreshToken,
                Duration.ofDays(REFRESH_TOKEN_EXPIRY_TIME.getExpiry()));
    }

    public String getRefreshToken(String id) {
        if (isExistKey(id)) {
            return redisTemplate.opsForValue().get(REFRESH_KEY + id);
        }

        throw new RefreshTokenNotFoundException();
    }

    public void deleteRefreshToken(String id) {
        redisTemplate.delete(REFRESH_KEY + id);
    }

    private boolean isExistKey(String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_KEY + id));
    }
}
