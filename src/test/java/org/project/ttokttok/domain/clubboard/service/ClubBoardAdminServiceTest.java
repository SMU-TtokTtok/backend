package org.project.ttokttok.domain.clubboard.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.FileIsNotImageException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.exception.ClubAdminNameNotMatchException;
import org.project.ttokttok.domain.clubboard.exception.ClubBoardNotFoundException;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.project.ttokttok.domain.clubboard.service.dto.request.ClubBoardUpdateServiceRequest;
import org.project.ttokttok.domain.clubboard.service.dto.request.CreateBoardServiceRequest;
import org.project.ttokttok.domain.clubboard.service.dto.request.DeleteBoardServiceRequest;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory.BOARD_IMAGE;

@ExtendWith(MockitoExtension.class)
class ClubBoardAdminServiceTest {

    private final ClubRepository clubRepository = mock(ClubRepository.class);
    private final ClubBoardRepository clubBoardRepository = mock(ClubBoardRepository.class);
    private final S3Service s3Service = mock(S3Service.class);
    private final ClubBoardAdminService clubBoardService =
            new ClubBoardAdminService(clubRepository, clubBoardRepository, s3Service);

    private static final String THUMBNAIL_URL = "https://cdn.example.com/board-images/uuid_thumb.png";

    private Club mockClub(String clubId) {
        Club club = mock(Club.class);
        when(club.getId()).thenReturn(clubId);
        return club;
    }

    private MultipartFile imageFile() {
        return new MockMultipartFile("thumbnail", "thumb.png", "image/png", "img".getBytes());
    }

    private MultipartFile pdfFile() {
        return new MockMultipartFile("thumbnail", "doc.pdf", "application/pdf", "pdf".getBytes());
    }

    @Nested
    @DisplayName("createBoard()")
    class CreateBoard {

        @Test
        @DisplayName("썸네일을 S3에 업로드하고 URL을 저장하며 게시글 생성에 성공한다.")
        void createBoardSuccess() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));
            when(s3Service.uploadFile(any(MultipartFile.class), eq(BOARD_IMAGE.getDirectoryName())))
                    .thenReturn(THUMBNAIL_URL);

            ClubBoard savedBoard = mock(ClubBoard.class);
            when(savedBoard.getId()).thenReturn("board123");
            when(clubBoardRepository.save(any(ClubBoard.class))).thenReturn(savedBoard);

            CreateBoardServiceRequest request =
                    new CreateBoardServiceRequest("admin", "club123", "title", "content", imageFile());

            String result = clubBoardService.createBoard(request);

            assertThat(result).isEqualTo("board123");
            verify(s3Service).uploadFile(any(MultipartFile.class), eq(BOARD_IMAGE.getDirectoryName()));
            verify(clubBoardRepository).save(any(ClubBoard.class));
            verify(s3Service, never()).deleteFile(anyString());
        }

        @Test
        @DisplayName("썸네일이 이미지 형식이 아니면 예외가 발생하고 업로드하지 않는다.")
        void createBoardNotImage() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            CreateBoardServiceRequest request =
                    new CreateBoardServiceRequest("admin", "club123", "title", "content", pdfFile());

            assertThatThrownBy(() -> clubBoardService.createBoard(request))
                    .isInstanceOf(FileIsNotImageException.class);

            verify(s3Service, never()).uploadFile(any(), anyString());
            verify(clubBoardRepository, never()).save(any());
        }

        @Test
        @DisplayName("DB 저장이 실패하면 업로드된 썸네일을 보상 삭제하고 예외를 다시 던진다.")
        void createBoardCompensatesUploadOnSaveFailure() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));
            when(s3Service.uploadFile(any(MultipartFile.class), eq(BOARD_IMAGE.getDirectoryName())))
                    .thenReturn(THUMBNAIL_URL);
            when(clubBoardRepository.save(any(ClubBoard.class)))
                    .thenThrow(new RuntimeException("db down"));

            CreateBoardServiceRequest request =
                    new CreateBoardServiceRequest("admin", "club123", "title", "content", imageFile());

            assertThatThrownBy(() -> clubBoardService.createBoard(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db down");

            verify(s3Service).deleteFile(THUMBNAIL_URL);
        }

        @Test
        @DisplayName("요청한 clubId가 관리자의 동아리와 다르면 예외가 발생한다.")
        void createBoardClubIdMismatch() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            CreateBoardServiceRequest request =
                    new CreateBoardServiceRequest("admin", "otherClub", "title", "content", imageFile());

            assertThatThrownBy(() -> clubBoardService.createBoard(request))
                    .isInstanceOf(ClubAdminNameNotMatchException.class);

            verify(s3Service, never()).uploadFile(any(), anyString());
            verify(clubBoardRepository, never()).save(any());
        }

        @Test
        @DisplayName("요청자가 동아리 관리자가 아니면 예외가 발생한다.")
        void createBoardNotAdmin() {
            when(clubRepository.findByAdminUsername("stranger")).thenReturn(Optional.empty());

            CreateBoardServiceRequest request =
                    new CreateBoardServiceRequest("stranger", "club123", "title", "content", imageFile());

            assertThatThrownBy(() -> clubBoardService.createBoard(request))
                    .isInstanceOf(NotClubAdminException.class);
        }
    }

    @Nested
    @DisplayName("updateBoard()")
    class UpdateBoard {

        @Test
        @DisplayName("일부 필드만 보내도 수정에 성공하고, 썸네일이 없으면 S3에 접근하지 않는다.")
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
            verifyNoInteractions(s3Service);
        }

        @Test
        @DisplayName("썸네일을 교체하면 새 이미지를 업로드하고 기존 파일을 삭제한다.")
        void updateBoardReplacesThumbnail() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            String oldUrl = "https://cdn.example.com/board-images/uuid_old.png";
            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(club);
            when(board.getThumbnailUrl()).thenReturn(oldUrl);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));
            when(s3Service.uploadFile(any(MultipartFile.class), eq(BOARD_IMAGE.getDirectoryName())))
                    .thenReturn(THUMBNAIL_URL);

            ClubBoardUpdateServiceRequest request = ClubBoardUpdateServiceRequest.builder()
                    .username("admin")
                    .clubId("club123")
                    .boardId("board123")
                    .thumbnail(imageFile())
                    .build();

            clubBoardService.updateBoard(request);

            verify(board).updateThumbnailUrl(THUMBNAIL_URL);
            verify(s3Service).deleteFile(oldUrl);
        }

        @Test
        @DisplayName("기존 썸네일 삭제가 실패해도 교체 요청은 성공한다.")
        void updateBoardOldThumbnailDeleteFailureIsIgnored() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            String oldUrl = "https://cdn.example.com/board-images/uuid_old.png";
            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(club);
            when(board.getThumbnailUrl()).thenReturn(oldUrl);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));
            when(s3Service.uploadFile(any(MultipartFile.class), eq(BOARD_IMAGE.getDirectoryName())))
                    .thenReturn(THUMBNAIL_URL);
            doThrow(new RuntimeException("s3 down")).when(s3Service).deleteFile(oldUrl);

            ClubBoardUpdateServiceRequest request = ClubBoardUpdateServiceRequest.builder()
                    .username("admin")
                    .clubId("club123")
                    .boardId("board123")
                    .thumbnail(imageFile())
                    .build();

            clubBoardService.updateBoard(request);

            verify(board).updateThumbnailUrl(THUMBNAIL_URL);
        }

        @Test
        @DisplayName("교체할 썸네일이 이미지 형식이 아니면 예외가 발생한다.")
        void updateBoardNotImage() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(club);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));

            ClubBoardUpdateServiceRequest request = ClubBoardUpdateServiceRequest.builder()
                    .username("admin")
                    .clubId("club123")
                    .boardId("board123")
                    .thumbnail(pdfFile())
                    .build();

            assertThatThrownBy(() -> clubBoardService.updateBoard(request))
                    .isInstanceOf(FileIsNotImageException.class);

            verify(s3Service, never()).uploadFile(any(), anyString());
            verify(board, never()).updateThumbnailUrl(anyString());
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
        @DisplayName("게시글 삭제 시 S3 썸네일 파일도 삭제한다.")
        void deleteBoardSuccess() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(club);
            when(board.getThumbnailUrl()).thenReturn(THUMBNAIL_URL);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));

            DeleteBoardServiceRequest request = new DeleteBoardServiceRequest("admin", "club123", "board123");

            clubBoardService.deleteBoard(request);

            verify(clubBoardRepository).delete(board);
            verify(s3Service).deleteFile(THUMBNAIL_URL);
        }

        @Test
        @DisplayName("썸네일이 없는 레거시 게시글은 S3에 접근하지 않고 삭제한다.")
        void deleteBoardWithoutThumbnail() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(club);
            when(board.getThumbnailUrl()).thenReturn(null);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));

            DeleteBoardServiceRequest request = new DeleteBoardServiceRequest("admin", "club123", "board123");

            clubBoardService.deleteBoard(request);

            verify(clubBoardRepository).delete(board);
            verifyNoInteractions(s3Service);
        }

        @Test
        @DisplayName("S3 삭제가 실패해도 게시글 삭제는 성공한다.")
        void deleteBoardS3FailureIsIgnored() {
            Club club = mockClub("club123");
            when(clubRepository.findByAdminUsername("admin")).thenReturn(Optional.of(club));

            ClubBoard board = mock(ClubBoard.class);
            when(board.getClub()).thenReturn(club);
            when(board.getThumbnailUrl()).thenReturn(THUMBNAIL_URL);
            when(clubBoardRepository.findById("board123")).thenReturn(Optional.of(board));
            doThrow(new RuntimeException("s3 down")).when(s3Service).deleteFile(THUMBNAIL_URL);

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
            verifyNoInteractions(s3Service);
        }
    }
}
