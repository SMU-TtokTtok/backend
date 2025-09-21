package org.project.ttokttok.domain.club.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailServiceResponse;
import org.project.ttokttok.global.config.ClubPopularityConfig;
import org.project.ttokttok.infrastructure.s3.service.S3Service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubUserServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private ClubPopularityConfig popularityConfig;

    @InjectMocks
    private ClubUserService clubUserService;

    @Test
    @DisplayName("동아리 상세 조회 시 조회수가 1 증가해야 한다")
    void getClubIntroduction_shouldIncreaseViewCount() {
        // given
        String username = "test@sangmyung.kr";
        String clubId = "test-club-id";

        Club mockClub = mock(Club.class);
        ClubDetailQueryResponse mockQueryResponse = mock(ClubDetailQueryResponse.class);
        ClubDetailServiceResponse mockServiceResponse = mock(ClubDetailServiceResponse.class);

        given(clubRepository.findById(clubId)).willReturn(Optional.of(mockClub));
        given(clubRepository.getClubIntroduction(clubId, username)).willReturn(mockQueryResponse);

        try (MockedStatic<ClubDetailServiceResponse> mockedStatic = mockStatic(ClubDetailServiceResponse.class)) {
            mockedStatic.when(() -> ClubDetailServiceResponse.from(mockQueryResponse))
                    .thenReturn(mockServiceResponse);

            // when
            ClubDetailServiceResponse result = clubUserService.getClubIntroduction(username, clubId);

            // then
            assertThat(result).isEqualTo(mockServiceResponse);
            verify(mockClub, times(1)).updateViewCount(); // 조회수 증가 메서드가 정확히 한 번 호출되었는지 확인
            verify(clubRepository, times(1)).findById(clubId);
            verify(clubRepository, times(1)).getClubIntroduction(clubId, username);
        }
    }

    @Test
    @DisplayName("존재하지 않는 동아리 조회 시 ClubNotFoundException이 발생해야 한다")
    void getClubIntroduction_shouldThrowExceptionWhenClubNotFound() {
        // given
        String username = "test@sangmyung.kr";
        String clubId = "non-existent-club-id";

        given(clubRepository.findById(clubId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> clubUserService.getClubIntroduction(username, clubId))
                .isInstanceOf(ClubNotFoundException.class);

        verify(clubRepository, times(1)).findById(clubId);
        verify(clubRepository, never()).getClubIntroduction(anyString(), anyString());
    }

    @Test
    @DisplayName("동일한 동아리를 여러 번 조회할 때마다 조회수가 증가해야 한다")
    void getClubIntroduction_shouldIncreaseViewCountOnMultipleCalls() {
        // given
        String username = "test@sangmyung.kr";
        String clubId = "test-club-id";

        Club mockClub = mock(Club.class);
        ClubDetailQueryResponse mockQueryResponse = mock(ClubDetailQueryResponse.class);
        ClubDetailServiceResponse mockServiceResponse = mock(ClubDetailServiceResponse.class);

        given(clubRepository.findById(clubId)).willReturn(Optional.of(mockClub));
        given(clubRepository.getClubIntroduction(clubId, username)).willReturn(mockQueryResponse);

        try (MockedStatic<ClubDetailServiceResponse> mockedStatic = mockStatic(ClubDetailServiceResponse.class)) {
            mockedStatic.when(() -> ClubDetailServiceResponse.from(mockQueryResponse))
                    .thenReturn(mockServiceResponse);

            // when
            clubUserService.getClubIntroduction(username, clubId);
            clubUserService.getClubIntroduction(username, clubId);
            clubUserService.getClubIntroduction(username, clubId);

            // then
            verify(mockClub, times(3)).updateViewCount(); // 3번 호출되었는지 확인
            verify(clubRepository, times(3)).findById(clubId);
            verify(clubRepository, times(3)).getClubIntroduction(clubId, username);
        }
    }

    @Test
    @DisplayName("서로 다른 사용자가 동일한 동아리를 조회할 때마다 조회수가 증가해야 한다")
    void getClubIntroduction_shouldIncreaseViewCountForDifferentUsers() {
        // given
        String user1 = "user1@sangmyung.kr";
        String user2 = "user2@sangmyung.kr";
        String clubId = "test-club-id";

        Club mockClub = mock(Club.class);
        ClubDetailQueryResponse mockQueryResponse = mock(ClubDetailQueryResponse.class);
        ClubDetailServiceResponse mockServiceResponse = mock(ClubDetailServiceResponse.class);

        given(clubRepository.findById(clubId)).willReturn(Optional.of(mockClub));
        given(clubRepository.getClubIntroduction(anyString(), anyString())).willReturn(mockQueryResponse);

        try (MockedStatic<ClubDetailServiceResponse> mockedStatic = mockStatic(ClubDetailServiceResponse.class)) {
            mockedStatic.when(() -> ClubDetailServiceResponse.from(mockQueryResponse))
                    .thenReturn(mockServiceResponse);

            // when
            clubUserService.getClubIntroduction(user1, clubId);
            clubUserService.getClubIntroduction(user2, clubId);

            // then
            verify(mockClub, times(2)).updateViewCount(); // 2번 호출되었는지 확인
            verify(clubRepository, times(2)).findById(clubId);
            verify(clubRepository, times(1)).getClubIntroduction(clubId, user1);
            verify(clubRepository, times(1)).getClubIntroduction(clubId, user2);
        }
    }
}
