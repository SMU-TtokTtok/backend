package org.project.ttokttok.infrastructure.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.global.auth.jwt.exception.AlreadyLogoutException;
import org.project.ttokttok.global.auth.jwt.exception.RefreshTokenAlreadyExistsException;
import org.project.ttokttok.global.auth.jwt.exception.RefreshTokenExpiredException;
import org.project.ttokttok.global.auth.jwt.exception.RefreshTokenNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.project.ttokttok.global.auth.jwt.TokenExpiry.REFRESH_TOKEN_EXPIRY_TIME;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    // Redis 기반 리프레시 토큰 CRUD 로직을 지원하는 클래스

    private final RedisTemplate<String, String> redisTemplate;

    // 레디스 리프레시 토큰 저장 키 접두사.
    private static final String REFRESH_REDIS_KEY = "refresh:";

    // 리프레시 토큰 저장 로직
    public void save(String username, String refreshToken) {
        if (isExistKey(username)) { // 리프레시 토큰이 존재하면 예외 발생
            throw new RefreshTokenAlreadyExistsException();
        }

        redisTemplate.opsForValue().set(
                REFRESH_REDIS_KEY + username, // 레디스 키 설정(refresh:사용자명)
                refreshToken, // 값 (리프레시 토큰)
                Duration.ofMillis(REFRESH_TOKEN_EXPIRY_TIME.getExpiry())); // TTL(만료 시간 7일)
    }

    // 리프레시 토큰 조회
    public String getRefreshToken(String username) {
        if (isExistKey(username)) {
            return redisTemplate.opsForValue().get(REFRESH_REDIS_KEY + username);
        }

        throw new RefreshTokenNotFoundException();
    }

    // 리프레시 토큰 삭제 로직 - 로그아웃 시
    public void deleteRefreshToken(String username) {
        // 추후 필요하다면, 액세스토큰 블랙리스트 로직 추가 고려하기
        if (isExistKey(username)) {
            redisTemplate.delete(REFRESH_REDIS_KEY + username);
            log.info("로그아웃 완료: {}, logout at: {}", username, LocalDateTime.now());
            return;
        }

        throw new AlreadyLogoutException();
    }

    // 액세스 토큰 리이슈 시 사용
    public Long updateRefreshToken(String username, String refreshToken) {
        // 토큰 남은 시간
        Long refreshTTL = redisTemplate.getExpire(REFRESH_REDIS_KEY + username, TimeUnit.MILLISECONDS);

        // 토큰 시간 검증
        tokenAliveValidate(refreshTTL);

        // 새로운 리프레시 토큰으로 다시 내려줌.
        redisTemplate.opsForValue().set(REFRESH_REDIS_KEY + username, refreshToken, refreshTTL);

        return refreshTTL; // 리프레시 토큰의 남은 TTL 반환
    }

    private void tokenAliveValidate(Long refreshTTL) {
        if (refreshTTL == null) {
            throw new RefreshTokenExpiredException();
        }
    }

    // 유효한 키인지 검증
    private boolean isExistKey(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_REDIS_KEY + username));
    }
}
