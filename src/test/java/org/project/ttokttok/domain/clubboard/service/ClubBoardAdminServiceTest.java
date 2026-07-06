package org.project.ttokttok.domain.clubboard.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.exception.ClubAdminNameNotMatchException;
import org.project.ttokttok.domain.clubboard.exception.ClubBoardNotFoundException;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.project.ttokttok.domain.clubboard.service.dto.request.ClubBoardUpdateServiceRequest;
import org.project.ttokttok.domain.clubboard.service.dto.request.CreateBoardServiceRequest;
import org.project.ttokttok.domain.clubboard.service.dto.request.DeleteBoardServiceRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubBoardAdminServiceTest {

    private final ClubRepository clubRepository = mock(ClubRepository.class);
    private final ClubBoardRepository clubBoardRepository = mock(ClubBoardRepository.class);
    private final ClubBoardAdminService clubBoardService =
            new ClubBoardAdminService(clubRepository, clubBoardRepository);

    private Club mockClub(String clubId) {
        Club club = mock(Club.class);
        when(club.getId()).thenReturn(clubId);
        return club;
    }

    @Nested
    @DisplayName("createBoard()")
    class CreateBoard {

        @Test
        @DisplayName("게시글 생성에 성공한다.")
        void createBoardSuccess() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            ClubBoard savedBoard = mock(ClubBoard.class);
            when(savedBoard.getId()).thenReturn("board123");
            when(clubBoardRepository.save(any(ClubBoard.class))).thenReturn(savedBoard);

            CreateBoardServiceRequest request = new CreateBoardServiceRequest("admin", "club123", "title", "content");

            String result = clubBoardService.createBoard(request);

            assertThat(result).isEqualTo("board123");
            verify(clubBoardRepository).save(any(ClubBoard.class));
        }

        @Test
        @DisplayName("요청한 clubId가 관리자의 동아리와 다르면 예외가 발생한다.")
        void createBoardClubIdMismatch() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            CreateBoardServiceRequest request = new CreateBoardServiceRequest("admin", "otherClub", "title", "content");

            assertThatThrownBy(() -> clubBoardService.createBoard(request))
                    .isInstanceOf(ClubAdminNameNotMatchException.class);

            verify(clubBoardRepository, never()).save(any());
        }

        @Test
        @DisplayName("요청자가 동아리 관리자가 아니면 예외가 발생한다.")
        void createBoardNotAdmin() {
            when(clubRepository.findByAdminUsername("stranger")).thenReturn(Optional.empty());

            CreateBoardServiceRequest request = new CreateBoardServiceRequest("stranger", "club123", "title", "content");

            assertThatThrownBy(() -> clubBoardService.createBoard(request))
                    .isInstanceOf(NotClubAdminException.class);
        }
    }

    @Nested
    @DisplayName("updateBoard()")
    class UpdateBoard {

        @Test
        @DisplayName("일부 필드만 보내도 수정에 성공한다.")
        void updateBoardPartialSuccess() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(club);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));

            ClubBoardUpdateServiceRequest request = ClubBoardUpdateServiceRequest.builder()
                    .username("admin")
                    .clubId("club123")
                    .boardId("board123")
                    .title("새 제목")
                    .content(null)
                    .build();

            clubBoardService.updateBoard(request);

            verify(board).update("새 제목", null);
        }

        @Test
        @DisplayName("게시글이 존재하지 않으면 예외가 발생한다.")
        void updateBoardNotFound() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));
            when(clubBoardRepository.findById("missing")).thenReturn(Optional.empty());

            ClubBoardUpdateServiceRequest request = ClubBoardUpdateServiceRequest.builder()
                    .username("admin")
                    .clubId("club123")
                    .boardId("missing")
                    .title("title")
                    .content("content")
                    .build();

            assertThatThrownBy(() -> clubBoardService.updateBoard(request))
                    .isInstanceOf(ClubBoardNotFoundException.class);
        }

        @Test
        @DisplayName("게시글이 다른 동아리 소속이면 예외가 발생한다.")
        void updateBoardClubMismatch() {
            Club myClub = mockClub("club123");
            Club otherClub = mockClub("otherClub");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(myClub));

            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(otherClub);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));

            ClubBoardUpdateServiceRequest request = ClubBoardUpdateServiceRequest.builder()
                    .username("admin")
                    .clubId("club123")
                    .boardId("board123")
                    .title("title")
                    .content("content")
                    .build();

            assertThatThrownBy(() -> clubBoardService.updateBoard(request))
                    .isInstanceOf(ClubAdminNameNotMatchException.class);

            verify(board, never()).update(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteBoard()")
    class DeleteBoard {

        @Test
        @DisplayName("게시글 삭제에 성공한다.")
        void deleteBoardSuccess() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(club);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));

            DeleteBoardServiceRequest request = new DeleteBoardServiceRequest("admin", "club123", "board123");

            clubBoardService.deleteBoard(request);

            verify(clubBoardRepository).delete(board);
        }

        @Test
        @DisplayName("게시글이 다른 동아리 소속이면 예외가 발생하고 삭제되지 않는다.")
        void deleteBoardClubMismatch() {
            Club myClub = mockClub("club123");
            Club otherClub = mockClub("otherClub");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(myClub));

            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(otherClub);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));

            DeleteBoardServiceRequest request = new DeleteBoardServiceRequest("admin", "club123", "board123");

            assertThatThrownBy(() -> clubBoardService.deleteBoard(request))
                    .isInstanceOf(ClubAdminNameNotMatchException.class);

            verify(clubBoardRepository, never()).delete(any());
        }
    }
}
