package org.project.ttokttok.domain.clubboard.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.club.domain.Club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ClubBoardTest {

    private static final String THUMBNAIL_URL = "https://cdn.example.com/board-images/uuid_thumb.png";

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("올바른 정보로 게시글을 생성할 수 있다.")
        void createSuccess() {
            Club club = mock(Club.class);

            ClubBoard board = ClubBoard.create("제목", "내용", THUMBNAIL_URL, club);

            assertThat(board.getTitle()).isEqualTo("제목");
            assertThat(board.getContent()).isEqualTo("내용");
            assertThat(board.getThumbnailUrl()).isEqualTo(THUMBNAIL_URL);
            assertThat(board.getClub()).isEqualTo(club);
            assertThat(board.getId()).isNull(); // JPA 저장 전이므로 null
        }

        @Test
        @DisplayName("제목이 비어있으면 예외가 발생한다.")
        void createFailBlankTitle() {
            Club club = mock(Club.class);

            assertThatThrownBy(() -> ClubBoard.create(" ", "내용", THUMBNAIL_URL, club))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Title cannot be null or blank.");
        }

        @Test
        @DisplayName("제목이 null이면 예외가 발생한다.")
        void createFailNullTitle() {
            Club club = mock(Club.class);

            assertThatThrownBy(() -> ClubBoard.create(null, "내용", THUMBNAIL_URL, club))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Title cannot be null or blank.");
        }

        @Test
        @DisplayName("내용이 비어있으면 예외가 발생한다.")
        void createFailBlankContent() {
            Club club = mock(Club.class);

            assertThatThrownBy(() -> ClubBoard.create("제목", " ", THUMBNAIL_URL, club))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Content cannot be null or blank.");
        }

        @Test
        @DisplayName("썸네일 URL이 null이면 예외가 발생한다.")
        void createFailNullThumbnailUrl() {
            Club club = mock(Club.class);

            assertThatThrownBy(() -> ClubBoard.create("제목", "내용", null, club))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Thumbnail URL cannot be null or blank.");
        }

        @Test
        @DisplayName("썸네일 URL이 비어있으면 예외가 발생한다.")
        void createFailBlankThumbnailUrl() {
            Club club = mock(Club.class);

            assertThatThrownBy(() -> ClubBoard.create("제목", "내용", " ", club))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Thumbnail URL cannot be null or blank.");
        }

        @Test
        @DisplayName("동아리가 null이면 예외가 발생한다.")
        void createFailNullClub() {
            assertThatThrownBy(() -> ClubBoard.create("제목", "내용", THUMBNAIL_URL, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Club cannot be null.");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        private ClubBoard newBoard() {
            return ClubBoard.create("원제목", "원내용", THUMBNAIL_URL, mock(Club.class));
        }

        @Test
        @DisplayName("제목과 내용을 모두 수정할 수 있다.")
        void updateBothFields() {
            ClubBoard board = newBoard();

            board.update("새 제목", "새 내용");

            assertThat(board.getTitle()).isEqualTo("새 제목");
            assertThat(board.getContent()).isEqualTo("새 내용");
        }

        @Test
        @DisplayName("title만 전달하면 content는 기존 값을 유지한다.")
        void updateTitleOnly() {
            ClubBoard board = newBoard();

            board.update("새 제목", null);

            assertThat(board.getTitle()).isEqualTo("새 제목");
            assertThat(board.getContent()).isEqualTo("원내용");
        }

        @Test
        @DisplayName("content만 전달하면 title은 기존 값을 유지한다.")
        void updateContentOnly() {
            ClubBoard board = newBoard();

            board.update(null, "새 내용");

            assertThat(board.getTitle()).isEqualTo("원제목");
            assertThat(board.getContent()).isEqualTo("새 내용");
        }

        @Test
        @DisplayName("둘 다 null이면 아무 것도 바뀌지 않는다.")
        void updateBothNull() {
            ClubBoard board = newBoard();

            board.update(null, null);

            assertThat(board.getTitle()).isEqualTo("원제목");
            assertThat(board.getContent()).isEqualTo("원내용");
        }

        @Test
        @DisplayName("빈 문자열로 수정을 시도하면 예외가 발생한다.")
        void updateBlankTitleThrows() {
            ClubBoard board = newBoard();

            assertThatThrownBy(() -> board.update(" ", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Title cannot be null or blank.");
        }
    }

    @Nested
    @DisplayName("updateThumbnailUrl()")
    class UpdateThumbnailUrl {

        @Test
        @DisplayName("썸네일 URL을 교체할 수 있다.")
        void updateThumbnailUrlSuccess() {
            ClubBoard board = ClubBoard.create("제목", "내용", THUMBNAIL_URL, mock(Club.class));
            String newUrl = "https://cdn.example.com/board-images/uuid_new.png";

            board.updateThumbnailUrl(newUrl);

            assertThat(board.getThumbnailUrl()).isEqualTo(newUrl);
        }

        @Test
        @DisplayName("null 또는 빈 문자열로 교체를 시도하면 예외가 발생한다.")
        void updateThumbnailUrlBlankThrows() {
            ClubBoard board = ClubBoard.create("제목", "내용", THUMBNAIL_URL, mock(Club.class));

            assertThatThrownBy(() -> board.updateThumbnailUrl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Thumbnail URL cannot be null or blank.");
            assertThatThrownBy(() -> board.updateThumbnailUrl(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Thumbnail URL cannot be null or blank.");
        }
    }
}
