package org.project.ttokttok.domain.clubboard.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardDetailResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse.ClubBoardSummary;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.exception.ClubBoardNotFoundException;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubBoardUserServiceTest {

    private final ClubBoardRepository clubBoardRepository = mock(ClubBoardRepository.class);
    private final ClubRepository clubRepository = mock(ClubRepository.class);
    private final ClubBoardUserService clubBoardUserService =
            new ClubBoardUserService(clubBoardRepository, clubRepository);

    private static final String THUMBNAIL_URL = "https://cdn.example.com/board-images/uuid_thumb.webp";

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
            assertThat(summary.createdAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
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
        @DisplayName("썸네일이 있는 게시글은 thumbnailUrl이 내려간다.")
        void getBoardListWithThumbnail() {
            when(clubRepository.existsById("club123")).thenReturn(true);

            ClubBoard board = mockBoard("board1", "제목", "내용");
            lenient().when(board.getThumbnailUrl()).thenReturn(THUMBNAIL_URL);
            when(clubBoardRepository.findBoardsByClubIdWithCursor("club123", 20, null))
                    .thenReturn(List.of(board));

            ClubBoardListResponse response = clubBoardUserService.getBoardList("club123", 20, null);

            assertThat(response.boards().get(0).thumbnailUrl()).isEqualTo(THUMBNAIL_URL);
        }

        @Test
        @DisplayName("썸네일이 없는 레거시 게시글은 thumbnailUrl이 null이다.")
        void getBoardListLegacyBoardWithoutThumbnail() {
            when(clubRepository.existsById("club123")).thenReturn(true);

            ClubBoard board = mockBoard("board1", "제목", "내용");
            when(clubBoardRepository.findBoardsByClubIdWithCursor("club123", 20, null))
                    .thenReturn(List.of(board));

            ClubBoardListResponse response = clubBoardUserService.getBoardList("club123", 20, null);

            assertThat(response.boards().get(0).thumbnailUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("getBoardDetail()")
    class GetBoardDetail {

        @Test
        @DisplayName("게시글 상세 정보를 조회한다.")
        void getBoardDetailSuccess() {
            ClubBoard board = mockBoard("board1", "제목", "본문 전체 내용");
            lenient().when(board.getThumbnailUrl()).thenReturn(THUMBNAIL_URL);
            when(clubBoardRepository.findByIdAndClubIdWithClub("board1", "club123"))
                    .thenReturn(Optional.of(board));

            ClubBoardDetailResponse response = clubBoardUserService.getBoardDetail("club123", "board1");

            assertThat(response.boardId()).isEqualTo("board1");
            assertThat(response.title()).isEqualTo("제목");
            assertThat(response.content()).isEqualTo("본문 전체 내용");
            assertThat(response.thumbnailUrl()).isEqualTo(THUMBNAIL_URL);
            assertThat(response.clubName()).isEqualTo("동아리");
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        }

        @Test
        @DisplayName("존재하지 않는 게시글을 조회하면 예외가 발생한다.")
        void getBoardDetailNotFound() {
            when(clubBoardRepository.findByIdAndClubIdWithClub("missing", "club123"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> clubBoardUserService.getBoardDetail("club123", "missing"))
                    .isInstanceOf(ClubBoardNotFoundException.class);
        }

        @Test
        @DisplayName("다른 동아리 소속 게시글을 조회하면 예외가 발생한다.")
        void getBoardDetailWrongClub() {
            // 리포지토리 쿼리가 clubId 조건을 포함하므로 다른 동아리의 boardId로는 조회되지 않는다.
            when(clubBoardRepository.findByIdAndClubIdWithClub("board1", "otherClub"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> clubBoardUserService.getBoardDetail("otherClub", "board1"))
                    .isInstanceOf(ClubBoardNotFoundException.class);
        }
    }
}
