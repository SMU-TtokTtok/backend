package org.project.ttokttok.infrastructure.redis;

import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final Environment environment;

    // YAML 파일 Redis 환경 설정 변수들
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    // 배포 환경 시
    private final String PROD = "prod";

    // Redis 클라이언트 생성 클래스 빈으로 등록
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        //Redis의 작동 방식 중 하나인 StandAlone 방식 이용
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();

        // Redis 연결 설정
        redisConfig.setHostName(Objects.requireNonNull(redisHost, "redisHost에 설정된 값이 null 입니다."));
        redisConfig.setPort(redisPort);

        if (isProdProfile() && !redisPassword.isEmpty()) {
            redisConfig.setPassword(RedisPassword.of(redisPassword));
        }

        // Redis 클라이언트 설정
        LettuceClientConfiguration lettuceClientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))  // 명시적으로 설정된 5가 null이 될 수는 없으니 예외처리 패스
                .build();

        return new LettuceConnectionFactory(redisConfig, lettuceClientConfig);
    }

    // Redis 템플릿 설정.
    // 당장은 사용하는 곳이 리프레시 토큰 쪽 밖에 없기에, key-value 타입에 value는
    // 단순 String으로 설정
    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        // 연결 설정으로, 기존에 등록해둔 빈 이용
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // 문자열 직렬화 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    // 현재 환경이 prod인지 체크하는 헬퍼 메서드
    private boolean isProdProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equals(PROD)) {
                return true;
            }
        }
        return false;
    }
}
