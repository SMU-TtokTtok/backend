package org.project.ttokttok.global.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 분산 스케줄러 락 설정.
 *
 * <p>블루-그린 무중단 배포에서는 전환 창 동안 blue/green 두 인스턴스가 동시에 떠 있다.
 * {@code @Scheduled} 는 인스턴스마다 독립적으로 동작하므로, 이 락이 없으면 매일 새벽 4시에
 * FCM 토큰 정리와 지원 폼 마감 처리가 <b>두 번</b> 실행된다.
 *
 * <p>락 저장소로는 이미 리프레시 토큰에 쓰고 있는 Redis 를 그대로 재사용한다
 * ({@link org.project.ttokttok.infrastructure.redis.RedisConfig} 의 커넥션 팩토리).
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    /** 락 키 접두사. dev/prod 가 같은 Redis 를 바라보더라도 락이 섞이지 않게 한다. */
    private static final String LOCK_ENVIRONMENT = "ttokttok";

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockProvider(redisConnectionFactory, LOCK_ENVIRONMENT);
    }
}
