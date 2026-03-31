package org.project.ttokttok.domain.clubboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.project.ttokttok.domain.clubboard.service.dto.request.CreateBoardServiceRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubBoardAdminServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubBoardRepository clubBoardRepository;

    @InjectMocks
    private ClubBoardAdminService clubBoardService;

    @Test
    @DisplayName("createBoard(): 게시글 생성 성공")
    void createBoardSuccess() {
        // given
        String clubId = "club123";
        Club mockClub = mock(Club.class);
        when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(mockClub));
        CreateBoardServiceRequest request = new CreateBoardServiceRequest("admin", clubId, "title", "content");

        ClubBoard mockBoard = mock(ClubBoard.class);
        when(mockBoard.getId()).thenReturn("board123");
        when(clubBoardRepository.save(any(ClubBoard.class))).thenReturn(mockBoard);

        // AOP 대신 수동으로 ClubHolder 설정
        

        try {
            // when
            String result = clubBoardService.createBoard(request);

            // then
            assertThat(result).isEqualTo("board123");
            verify(clubBoardRepository).save(any(ClubBoard.class));
        } finally {
            
        }
    }
}
