package org.project.ttokttok.global.redis;

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

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.config.activate.on-profile}")
    private String activeProfile;

    private final String PROD = "prod";

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();

        if (activeProfile.equals(PROD)) {
            // 운영 환경에서만 비밀번호 사용
            if (!redisPassword.isEmpty()) {
                redisConfig.setPassword(RedisPassword.of(redisPassword));
            }
        }

        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        LettuceClientConfiguration lettuceClientConfig = createLettuceClientConfig();

        return new LettuceConnectionFactory(redisConfig, lettuceClientConfig);
    }

    private LettuceClientConfiguration createLettuceClientConfig() {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(5));

        if (activeProfile.equals(PROD)) {
            builder.useSsl();
        }

        return builder.build();
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // 문자열 직렬화 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}
