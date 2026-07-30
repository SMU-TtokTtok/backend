package org.project.ttokttok.domain.club.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailServiceResponse;
import org.project.ttokttok.global.config.ClubPopularityConfig;
import org.project.ttokttok.infrastructure.s3.service.S3Service;

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
    @DisplayName("동아리 상세 조회 시 조회수가 원자적 UPDATE 로 1 증가한다")
    void getClubIntroduction_shouldIncreaseViewCount() {
        // given
        String username = "test@sangmyung.kr";
        String clubId = "test-club-id";

        ClubDetailQueryResponse mockQueryResponse = mock(ClubDetailQueryResponse.class);
        ClubDetailServiceResponse mockServiceResponse = mock(ClubDetailServiceResponse.class);

        given(clubRepository.getClubIntroduction(clubId, username)).willReturn(mockQueryResponse);

        try (MockedStatic<ClubDetailServiceResponse> mockedStatic = mockStatic(ClubDetailServiceResponse.class)) {
            mockedStatic.when(() -> ClubDetailServiceResponse.from(mockQueryResponse))
                    .thenReturn(mockServiceResponse);

            // when
            ClubDetailServiceResponse result = clubUserService.getClubIntroduction(username, clubId);

            // then
            assertThat(result).isEqualTo(mockServiceResponse);
            verify(clubRepository, times(1)).increaseViewCount(clubId);
            verify(clubRepository, times(1)).getClubIntroduction(clubId, username);
            // 엔티티를 읽어 조회수를 올리던 방식(read-modify-write)이 제거되었는지 확인
            verify(clubRepository, never()).findById(anyString());
        }
    }

    @Test
    @DisplayName("조회수 증가는 상세 조회보다 뒤에 실행된다")
    void getClubIntroduction_shouldIncreaseViewCountAfterReading() {
        // 행 배타 락이 커밋까지 유지되므로, 증가를 앞에 두면 뒤따르는 조회 쿼리가 락 구간에 들어간다.
        // 순서가 뒤바뀌면 처리량이 절반으로 떨어지므로 순서 자체를 고정한다.
        // given
        String username = "test@sangmyung.kr";
        String clubId = "test-club-id";

        ClubDetailQueryResponse mockQueryResponse = mock(ClubDetailQueryResponse.class);
        ClubDetailServiceResponse mockServiceResponse = mock(ClubDetailServiceResponse.class);

        given(clubRepository.getClubIntroduction(clubId, username)).willReturn(mockQueryResponse);

        try (MockedStatic<ClubDetailServiceResponse> mockedStatic = mockStatic(ClubDetailServiceResponse.class)) {
            mockedStatic.when(() -> ClubDetailServiceResponse.from(mockQueryResponse))
                    .thenReturn(mockServiceResponse);

            // when
            clubUserService.getClubIntroduction(username, clubId);

            // then
            InOrder inOrder = inOrder(clubRepository);
            inOrder.verify(clubRepository).getClubIntroduction(clubId, username);
            inOrder.verify(clubRepository).increaseViewCount(clubId);
        }
    }

    @Test
    @DisplayName("존재하지 않는 동아리 조회 시 ClubNotFoundException이 발생하고 조회수를 올리지 않는다")
    void getClubIntroduction_shouldThrowExceptionWhenClubNotFound() {
        // given
        String username = "test@sangmyung.kr";
        String clubId = "non-existent-club-id";

        // 존재하지 않는 동아리에 대해 리포지토리는 null 을 반환한다
        given(clubRepository.getClubIntroduction(clubId, username)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> clubUserService.getClubIntroduction(username, clubId))
                .isInstanceOf(ClubNotFoundException.class);

        verify(clubRepository, times(1)).getClubIntroduction(clubId, username);
        verify(clubRepository, never()).increaseViewCount(anyString());
    }

    @Test
    @DisplayName("동일한 동아리를 여러 번 조회하면 조회할 때마다 조회수가 증가한다")
    void getClubIntroduction_shouldIncreaseViewCountOnMultipleCalls() {
        // given
        String username = "test@sangmyung.kr";
        String clubId = "test-club-id";

        ClubDetailQueryResponse mockQueryResponse = mock(ClubDetailQueryResponse.class);
        ClubDetailServiceResponse mockServiceResponse = mock(ClubDetailServiceResponse.class);

        given(clubRepository.getClubIntroduction(clubId, username)).willReturn(mockQueryResponse);

        try (MockedStatic<ClubDetailServiceResponse> mockedStatic = mockStatic(ClubDetailServiceResponse.class)) {
            mockedStatic.when(() -> ClubDetailServiceResponse.from(mockQueryResponse))
                    .thenReturn(mockServiceResponse);

            // when
            clubUserService.getClubIntroduction(username, clubId);
            clubUserService.getClubIntroduction(username, clubId);
            clubUserService.getClubIntroduction(username, clubId);

            // then
            verify(clubRepository, times(3)).increaseViewCount(clubId);
            verify(clubRepository, times(3)).getClubIntroduction(clubId, username);
        }
    }

    @Test
    @DisplayName("서로 다른 사용자가 동일한 동아리를 조회할 때마다 조회수가 증가한다")
    void getClubIntroduction_shouldIncreaseViewCountForDifferentUsers() {
        // given
        String user1 = "user1@sangmyung.kr";
        String user2 = "user2@sangmyung.kr";
        String clubId = "test-club-id";

        ClubDetailQueryResponse mockQueryResponse = mock(ClubDetailQueryResponse.class);
        ClubDetailServiceResponse mockServiceResponse = mock(ClubDetailServiceResponse.class);

        given(clubRepository.getClubIntroduction(anyString(), anyString())).willReturn(mockQueryResponse);

        try (MockedStatic<ClubDetailServiceResponse> mockedStatic = mockStatic(ClubDetailServiceResponse.class)) {
            mockedStatic.when(() -> ClubDetailServiceResponse.from(mockQueryResponse))
                    .thenReturn(mockServiceResponse);

            // when
            clubUserService.getClubIntroduction(user1, clubId);
            clubUserService.getClubIntroduction(user2, clubId);

            // then
            verify(clubRepository, times(2)).increaseViewCount(clubId);
            verify(clubRepository, times(1)).getClubIntroduction(clubId, user1);
            verify(clubRepository, times(1)).getClubIntroduction(clubId, user2);
        }
    }
}
