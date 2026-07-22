package org.project.ttokttok.domain.clubboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.project.ttokttok.infrastructure.s3.support.AllowedFileTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory.BOARD_IMAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubBoardAdminService {

    private final ClubRepository clubRepository;
    private final ClubBoardRepository clubBoardRepository;
    private final S3Service s3Service;

    // 게시글 생성 (썸네일 이미지 필수)
    @Transactional
    public String createBoard(CreateBoardServiceRequest request) {
        Club club = validateClubAdmin(request.adminName(), request.clubId());

        validateImage(request.thumbnail());

        // S3 업로드를 먼저 수행하고, 이후 DB 저장이 실패하면 업로드본을 보상 삭제한다.
        String thumbnailUrl = s3Service.uploadFile(request.thumbnail(), BOARD_IMAGE.getDirectoryName());

        try {
            ClubBoard clubBoard = ClubBoard.create(request.title(), request.content(), thumbnailUrl, club);

            return clubBoardRepository.save(clubBoard)
                    .getId();
        } catch (RuntimeException e) {
            deleteFileQuietly(thumbnailUrl);
            throw e;
        }
    }

    // 게시글 수정 로직 (썸네일 교체 지원)
    @Transactional
    public void updateBoard(ClubBoardUpdateServiceRequest request) {
        Club club = validateClubAdmin(request.username(), request.clubId());

        ClubBoard clubBoard = findClubBoardOfClub(request.boardId(), club.getId());

        clubBoard.update(request.title(), request.content());

        if (hasThumbnail(request.thumbnail())) {
            replaceThumbnail(clubBoard, request.thumbnail());
        }
    }

    // 게시글 삭제 로직 (S3 썸네일도 best-effort 삭제)
    @Transactional
    public void deleteBoard(DeleteBoardServiceRequest request) {
        Club club = validateClubAdmin(request.adminName(), request.clubId());

        ClubBoard clubBoard = findClubBoardOfClub(request.boardId(), club.getId());

        String thumbnailUrl = clubBoard.getThumbnailUrl();

        // 게시글 삭제 로직
        clubBoardRepository.delete(clubBoard);

        // S3 삭제 실패가 게시글 삭제를 막지 않도록 best-effort로 처리한다.
        if (thumbnailUrl != null) {
            deleteFileQuietly(thumbnailUrl);
        }
    }

    private void replaceThumbnail(ClubBoard clubBoard, MultipartFile thumbnail) {
        validateImage(thumbnail);

        String oldThumbnailUrl = clubBoard.getThumbnailUrl();
        String newThumbnailUrl = s3Service.uploadFile(thumbnail, BOARD_IMAGE.getDirectoryName());

        clubBoard.updateThumbnailUrl(newThumbnailUrl);

        // 교체 성공 시 기존 파일은 best-effort 삭제 (실패해도 요청은 성공 처리)
        if (oldThumbnailUrl != null) {
            deleteFileQuietly(oldThumbnailUrl);
        }
    }

    private void deleteFileQuietly(String fileUrl) {
        try {
            s3Service.deleteFile(fileUrl);
        } catch (RuntimeException e) {
            log.warn("S3 썸네일 파일 삭제 실패 (best-effort): {}", fileUrl, e);
        }
    }

    private boolean hasThumbnail(MultipartFile thumbnail) {
        return thumbnail != null && !thumbnail.isEmpty();
    }

    private void validateImage(MultipartFile thumbnail) {
        // 허용 이미지 형식은 AllowedFileTypes 단일 소스를 참조한다.
        if (thumbnail == null || thumbnail.getContentType() == null
                || !AllowedFileTypes.IMAGES.contains(thumbnail.getContentType())) {
            throw new FileIsNotImageException();
        }
    }

    private ClubBoard findClubBoardOfClub(String boardId, String clubId) {
        ClubBoard clubBoard = clubBoardRepository.findById(boardId)
                .orElseThrow(ClubBoardNotFoundException::new);

        // 해당 게시글이 요청한 동아리의 게시글인지 추가 확인
        if (!clubBoard.getClub().getId().equals(clubId)) {
            throw new ClubAdminNameNotMatchException();
        }

        return clubBoard;
    }

    private Club validateClubAdmin(String username, String requestClubId) {
        Club club = clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);

        if (!club.getId().equals(requestClubId)) {
            throw new ClubAdminNameNotMatchException();
        }

        return club;
    }
}
