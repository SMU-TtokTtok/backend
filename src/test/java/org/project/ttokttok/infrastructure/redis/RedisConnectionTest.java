package org.project.ttokttok.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @DisplayName("Redis 커넥션이 존재한다면, 테스트에 성공한다.")
    @Test
    void redisConnectionTest() {
        //given
        final String testKey = "key";
        final String testValue = "value";

        //when
        redisTemplate.opsForValue().set(testKey, testValue);
        String result = redisTemplate.opsForValue().get(testKey);

        //then
        assertThat(result).isEqualTo(testValue);
    }
}
