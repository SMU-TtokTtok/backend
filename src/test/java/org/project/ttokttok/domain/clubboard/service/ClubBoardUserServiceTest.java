package org.project.ttokttok.domain.clubboard.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse.ClubBoardSummary;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubBoardUserServiceTest {

    private final ClubBoardRepository clubBoardRepository = mock(ClubBoardRepository.class);
    private final ClubRepository clubRepository = mock(ClubRepository.class);
    private final ClubBoardUserService clubBoardUserService =
            new ClubBoardUserService(clubBoardRepository, clubRepository);

    private ClubBoard mockBoard(String id, String title, String content) {
        Club club = mock(Club.class);
        lenient().when(club.getName()).thenReturn("동아리");

        ClubBoard board = mock(ClubBoard.class);
        lenient().when(board.getId()).thenReturn(id);
        lenient().when(board.getTitle()).thenReturn(title);
        lenient().when(board.getContent()).thenReturn(content);
        lenient().when(board.getClub()).thenReturn(club);
        lenient().when(board.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
        return board;
    }

    @Nested
    @DisplayName("getBoardList()")
    class GetBoardList {

        @Test
        @DisplayName("존재하지 않는 동아리를 조회하면 예외가 발생한다.")
        void getBoardListClubNotFound() {
            when(clubRepository.existsById("missing")).thenReturn(false);

            assertThatThrownBy(() -> clubBoardUserService.getBoardList("missing", 20, null))
                    .isInstanceOf(ClubNotFoundException.class);

            verify(clubBoardRepository, never()).findBoardsByClubIdWithCursor(any(), anyInt(), any());
        }

        @Test
        @DisplayName("다음 페이지가 없으면 hasNext가 false이고 nextCursor는 null이다.")
        void getBoardListNoNextPage() {
            when(clubRepository.existsById("club123")).thenReturn(true);

            ClubBoard board1 = mockBoard("board1", "제목1", "내용1");
            when(clubBoardRepository.findBoardsByClubIdWithCursor("club123", 20, null))
                    .thenReturn(List.of(board1));

            ClubBoardListResponse response = clubBoardUserService.getBoardList("club123", 20, null);

            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
            assertThat(response.boards()).hasSize(1);

            ClubBoardSummary summary = response.boards().get(0);
            assertThat(summary.boardId()).isEqualTo("board1");
            assertThat(summary.title()).isEqualTo("제목1");
            assertThat(summary.clubName()).isEqualTo("동아리");
        }

        @Test
        @DisplayName("조회된 개수가 size보다 많으면 hasNext가 true이고 마지막 항목이 잘려나간다.")
        void getBoardListHasNextPage() {
            when(clubRepository.existsById("club123")).thenReturn(true);

            ClubBoard board1 = mockBoard("board1", "제목1", "내용1");
            ClubBoard board2 = mockBoard("board2", "제목2", "내용2");
            // size가 1이지만 2개(size+1)를 반환하여 다음 페이지 존재를 알린다.
            when(clubBoardRepository.findBoardsByClubIdWithCursor("club123", 1, null))
                    .thenReturn(List.of(board1, board2));

            ClubBoardListResponse response = clubBoardUserService.getBoardList("club123", 1, null);

            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo("board1");
            assertThat(response.boards()).hasSize(1);
            assertThat(response.boards().get(0).boardId()).isEqualTo("board1");
        }

        @Test
        @DisplayName("게시글 내용에 img 태그가 있으면 hasImages가 true이다.")
        void getBoardListHasImagesFromImgTag() {
            when(clubRepository.existsById("club123")).thenReturn(true);

            ClubBoard board = mockBoard("board1", "제목", "<p>본문 <img src='a.png'/></p>");
            when(clubBoardRepository.findBoardsByClubIdWithCursor("club123", 20, null))
                    .thenReturn(List.of(board));

            ClubBoardListResponse response = clubBoardUserService.getBoardList("club123", 20, null);

            assertThat(response.boards().get(0).hasImages()).isTrue();
        }

        @Test
        @DisplayName("게시글 내용에 이미지가 없으면 hasImages가 false이다.")
        void getBoardListNoImages() {
            when(clubRepository.existsById("club123")).thenReturn(true);

            ClubBoard board = mockBoard("board1", "제목", "그냥 텍스트 내용입니다.");
            when(clubBoardRepository.findBoardsByClubIdWithCursor("club123", 20, null))
                    .thenReturn(List.of(board));

            ClubBoardListResponse response = clubBoardUserService.getBoardList("club123", 20, null);

            assertThat(response.boards().get(0).hasImages()).isFalse();
        }
    }
}
