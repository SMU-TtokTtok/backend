package org.project.ttokttok.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.PopularClubCandidate;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("PopularClubCandidateCache - 인기 후보 Redis 캐시와 스탬피드 방지")
class PopularClubCandidateCacheTest {

    private static final ObjectMapper MAPPER = PopularClubCandidateCache.OBJECT_MAPPER;

    private static final List<PopularClubCandidate> CACHED = List.of(candidate("cached"));
    private static final List<PopularClubCandidate> FROM_DB = List.of(candidate("db"));

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> values;
    @Mock
    private ClubRepository clubRepository;
    @InjectMocks
    private PopularClubCandidateCache cache;

    @Test
    @DisplayName("갱신 시각 전이면 캐시 값을 쓰고 DB 를 읽지 않는다")
    void freshHit() throws Exception {
        givenCached(System.currentTimeMillis() + 60_000);

        assertThat(ids(cache.get())).containsExactly("cached");
        verify(clubRepository, never()).findPopularClubCandidates();
        verify(values, never()).setIfAbsent(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("갱신 시각이 지났고 락을 잡으면 DB 를 한 번 읽어 다시 채우고 락을 푼다")
    void staleWithLockRefreshes() throws Exception {
        givenCached(System.currentTimeMillis() - 60_000);
        given(values.setIfAbsent(eq(PopularClubCandidateCache.LOCK_KEY), anyString(), any())).willReturn(true);
        given(clubRepository.findPopularClubCandidates()).willReturn(FROM_DB);

        assertThat(ids(cache.get())).containsExactly("db");
        verify(clubRepository).findPopularClubCandidates();
        verify(values).set(eq(PopularClubCandidateCache.KEY), anyString(), eq(PopularClubCandidateCache.HARD_TTL));
        verify(redisTemplate).delete(PopularClubCandidateCache.LOCK_KEY);
    }

    @Test
    @DisplayName("갱신 시각이 지났어도 다른 요청이 락을 잡고 있으면 기존 값을 그대로 쓴다")
    void staleWithoutLockServesStale() throws Exception {
        givenCached(System.currentTimeMillis() - 60_000);
        given(values.setIfAbsent(eq(PopularClubCandidateCache.LOCK_KEY), anyString(), any())).willReturn(false);

        assertThat(ids(cache.get())).containsExactly("cached");
        verify(clubRepository, never()).findPopularClubCandidates();
    }

    @Test
    @DisplayName("캐시가 비었고 다른 요청이 채우는 중이면 기다리지 않고 DB 를 직접 읽는다")
    void coldWithoutLockReadsDb() {
        given(redisTemplate.opsForValue()).willReturn(values);
        given(values.get(PopularClubCandidateCache.KEY)).willReturn(null);
        given(values.setIfAbsent(eq(PopularClubCandidateCache.LOCK_KEY), anyString(), any())).willReturn(false);
        given(clubRepository.findPopularClubCandidates()).willReturn(FROM_DB);

        assertThat(ids(cache.get())).containsExactly("db");
        verify(values, never()).set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Redis 가 죽어 있으면 요청을 실패시키지 않고 DB 에서 읽는다")
    void redisDownFallsBackToDb() {
        given(redisTemplate.opsForValue()).willReturn(values);
        given(values.get(PopularClubCandidateCache.KEY)).willThrow(new RedisConnectionFailureException("down"));
        given(clubRepository.findPopularClubCandidates()).willReturn(FROM_DB);

        assertThat(ids(cache.get())).containsExactly("db");
        verify(clubRepository).findPopularClubCandidates();
    }

    @Test
    @DisplayName("저장한 값을 다시 읽으면 카드와 점수 재료가 그대로다")
    void roundTrip() throws Exception {
        PopularClubCandidateCache.Entry entry = new PopularClubCandidateCache.Entry(1L, CACHED);

        PopularClubCandidateCache.Entry read = MAPPER.readValue(MAPPER.writeValueAsString(entry),
                PopularClubCandidateCache.Entry.class);

        assertThat(read).isEqualTo(entry);
    }

    private void givenCached(long refreshAt) throws Exception {
        given(redisTemplate.opsForValue()).willReturn(values);
        given(values.get(PopularClubCandidateCache.KEY)).willReturn(json(refreshAt));
    }

    private static String json(long refreshAt) throws Exception {
        return MAPPER.writeValueAsString(new PopularClubCandidateCache.Entry(refreshAt, CACHED));
    }

    private static List<String> ids(List<PopularClubCandidate> candidates) {
        return candidates.stream().map(PopularClubCandidate::id).toList();
    }

    private static PopularClubCandidate candidate(String id) {
        ClubCardQueryResponse card = new ClubCardQueryResponse(id, "동아리" + id, ClubType.CENTRAL, ClubCategory.ACADEMIC,
                null, "요약", null, 10, true, false, false, java.time.LocalDate.of(2026, 11, 30));
        return new PopularClubCandidate(card, 3, 100, LocalDateTime.of(2026, 1, 1, 0, 0));
    }
}
