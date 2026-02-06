package org.project.ttokttok.infrastructure.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.global.auth.jwt.exception.AlreadyLogoutException;
import org.project.ttokttok.global.auth.jwt.exception.RefreshTokenExpiredException;
import org.project.ttokttok.global.auth.jwt.exception.RefreshTokenNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    // 액세스 토큰 블랙리스트 키 접두사
    private static final String ACCESS_BLACKLIST_KEY = "blacklist:access:";

    // 리프레시 토큰 저장 로직
    public void save(String username, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_REDIS_KEY + refreshToken, // 레디스 키 설정(refresh:리프레시 토큰)
                username, // 값 (사용자 명, email)
                Duration.ofMillis(REFRESH_TOKEN_EXPIRY_TIME.getExpiry())); // TTL(만료 시간 7일)
    }

    // 리프레시 토큰 조회
    public String getUsernameFromRefreshToken(String refreshToken) {
        if (isExistKey(refreshToken)) {
            return redisTemplate.opsForValue().get(REFRESH_REDIS_KEY + refreshToken);
        }

        throw new RefreshTokenNotFoundException();
    }

    // 리프레시 토큰 삭제 로직 - 로그아웃 시
    public void deleteRefreshToken(String refreshToken) {
        // 추후 필요하다면, 액세스토큰 블랙리스트 로직 추가 고려하기
        if (isExistKey(refreshToken)) {
            redisTemplate.delete(REFRESH_REDIS_KEY + refreshToken);
            //log.info("로그아웃 완료: {}, logout at: {}", username, LocalDateTime.now());
            return;
        }

        // 이미 로그아웃 되어있으면 그냥 ok 리턴
        //throw new AlreadyLogoutException();
    }

    // 액세스 토큰 블랙리스트 추가 - 로그아웃 시
    public void addAccessTokenToBlacklist(String accessToken, long expiryTime) {
        // 액세스 토큰의 남은 만료 시간만큼 블랙리스트에 저장
        redisTemplate.opsForValue().set(
                ACCESS_BLACKLIST_KEY + accessToken,
                "blacklisted",
                Duration.ofMillis(expiryTime)
        );
        log.info("액세스 토큰 블랙리스트 추가: {}", accessToken);
    }

    // 액세스 토큰이 블랙리스트에 있는지 확인
    public boolean isAccessTokenBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_BLACKLIST_KEY + accessToken));
    }

    // 로그아웃 시 리프레시 토큰 삭제 + 액세스 토큰 블랙리스트 추가
    public void logout(String refreshToken, String accessToken, long accessTokenExpiryTime) {

        // 리프레시 토큰 삭제
        if (isExistKey(refreshToken)) {
            redisTemplate.delete(REFRESH_REDIS_KEY + refreshToken);
        }

        // 액세스 토큰 블랙리스트 추가
        if (accessToken != null) {
            addAccessTokenToBlacklist(accessToken, accessTokenExpiryTime);
        }

        //log.info("로그아웃 완료: {}, logout at: {}", username, LocalDateTime.now());
    }

    // 액세스 토큰 리이슈 시 사용
    public Long getRefreshTTL(String refreshToken) {
        // 토큰 남은 시간
        Long refreshTTL = redisTemplate.getExpire(REFRESH_REDIS_KEY + refreshToken, TimeUnit.MILLISECONDS);

        // 토큰 시간 검증
        tokenAliveValidate(refreshTTL);

        return refreshTTL; // 리프레시 토큰의 남은 TTL 반환
    }

    private void tokenAliveValidate(Long refreshTTL) {
        if (refreshTTL == null) {
            throw new RefreshTokenExpiredException();
        }
    }

    // 유효한 키인지 검증
    private boolean isExistKey(String refreshToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_REDIS_KEY + refreshToken));
    }
}
