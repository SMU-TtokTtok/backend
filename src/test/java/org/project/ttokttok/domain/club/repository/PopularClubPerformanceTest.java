package org.project.ttokttok.domain.club.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import java.util.List;

public class PopularClubPerformanceTest implements RepositoryTestSupport {

    @Autowired
    private ClubRepository clubRepository;

    @Test
    @DisplayName("인기 동아리 조회 성능 측정")
    void measurePopularClubQueryPerformance() {
        // Given
        int size = 10;
        String cursor = null;
        String sort = "popular";
        String userEmail = null;
        double minScore = 7.0;

        StopWatch stopWatch = new StopWatch();

        // When
        stopWatch.start();
        List<ClubCardQueryResponse> results = clubRepository.getPopularClubsWithFilters(
                size, cursor, sort, userEmail, minScore
        );
        stopWatch.stop();

        // Then
        System.out.println("====================================================");
        System.out.println("인기 동아리 조회 쿼리 소요 시간: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("조회된 동아리 수: " + results.size());
        System.out.println("====================================================");
    }
}
