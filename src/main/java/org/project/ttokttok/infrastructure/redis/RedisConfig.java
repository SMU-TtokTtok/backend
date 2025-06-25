package org.project.ttokttok.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    // YAML 파일 Redis 환경 설정 변수들
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.config.activate.on-profile}")
    private String activeProfile;

    // 배포 환경 시
    private final String PROD = "prod";

    // Redis 클라이언트 생성 클래스 빈으로 등록
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        //Redis의 작동 방식 중 하나인 StandAlone 방식 이용
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();

        if (activeProfile.equals(PROD)) {
            // 운영 환경에서만 비밀번호 사용
            if (!redisPassword.isEmpty()) {
                redisConfig.setPassword(RedisPassword.of(redisPassword));
            }
        }

        // Redis 연결 설정
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        // Redis 클라이언트 설정
        LettuceClientConfiguration lettuceClientConfig = createLettuceClientConfig();

        return new LettuceConnectionFactory(redisConfig, lettuceClientConfig);
    }

    private LettuceClientConfiguration createLettuceClientConfig() {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(5));

        // 운영환경일 때만 암호화 등록
        if (activeProfile.equals(PROD)) {
            builder.useSsl();
        }

        return builder.build();
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
}
