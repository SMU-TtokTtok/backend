package org.project.ttokttok.domain.club.service;

import java.time.Duration;
import java.util.List;

import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.repository.dto.PopularClubCandidate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인기 목록 후보 전체를 Redis 에 캐시한다.
 *
 * <p>후보는 사용자와 무관하다(북마크는 서비스가 요청마다 덧씌운다). 그래서 키 하나에 전체를 담는다.
 * Redis 는 앱 밖에 있어 블루-그린 재배포 뒤에도 캐시가 남는다.
 *
 * <p>스탬피드 방지: 값에 갱신 시각({@link #REFRESH_AFTER})을 함께 저장하고, 실제 만료({@link #HARD_TTL})는 훨씬 길게 둔다.
 * 갱신 시각이 지나면 락({@code SET NX})을 잡은 요청 하나만 DB 를 다시 읽고, 나머지는 기존 값을 그대로 쓴다.
 * 값이 아예 없을 때(최초, 장기 미사용, Redis 초기화)는 락을 잡은 하나가 채우는 동안(쿼리 한 번)만 나머지도 DB 를 직접 읽는다.
 * Redis 장애나 역직렬화 실패는 요청을 실패시키지 않고 DB 로 직접 읽는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularClubCandidateCache {

    public static final String KEY = "club:popular:candidates";
    static final String LOCK_KEY = KEY + ":lock";
    static final Duration REFRESH_AFTER = Duration.ofSeconds(30);
    static final Duration HARD_TTL = Duration.ofMinutes(10);
    static final Duration LOCK_TTL = Duration.ofSeconds(5);

    /** 저장 형식은 이 캐시만 읽고 쓴다. 전역 ObjectMapper 빈 설정에 끌려가지 않게 따로 둔다. */
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final RedisTemplate<String, String> redisTemplate;
    private final ClubRepository clubRepository;

    List<PopularClubCandidate> get() {
        try {
            Entry cached = read();
            if (cached != null && System.currentTimeMillis() < cached.refreshAt()) {
                return cached.candidates();
            }
            if (tryLock()) {
                return refresh();
            }
            return cached != null ? cached.candidates() : clubRepository.findPopularClubCandidates();
        } catch (RuntimeException e) {
            log.warn("[PopularClubCandidateCache] 캐시를 쓰지 못해 DB 에서 읽는다 - error: {}", e.getClass().getSimpleName());
            return clubRepository.findPopularClubCandidates();
        }
    }

    private List<PopularClubCandidate> refresh() {
        try {
            List<PopularClubCandidate> candidates = clubRepository.findPopularClubCandidates();
            write(new Entry(System.currentTimeMillis() + REFRESH_AFTER.toMillis(), candidates));
            return candidates;
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }

    private boolean tryLock() {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL));
    }

    private Entry read() {
        String json = redisTemplate.opsForValue().get(KEY);
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, Entry.class);
        } catch (JsonProcessingException e) {
            // 모양이 바뀐 옛 값이다. 없는 것으로 보고 새로 채운다.
            return null;
        }
    }

    private void write(Entry entry) {
        try {
            redisTemplate.opsForValue().set(KEY, OBJECT_MAPPER.writeValueAsString(entry), HARD_TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Redis 에 저장하는 값. {@code refreshAt} 은 epoch 밀리초다. */
    record Entry(long refreshAt, List<PopularClubCandidate> candidates) {
    }
}
