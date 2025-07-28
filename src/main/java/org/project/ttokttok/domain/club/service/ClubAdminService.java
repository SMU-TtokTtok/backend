package org.project.ttokttok.domain.club.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.exception.InvalidDateRangeException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.FileIsNotImageException;
import org.project.ttokttok.domain.club.exception.NoApplyFormExistException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;
import org.project.ttokttok.domain.club.service.dto.request.MarkdownImageUpdateRequest;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailAdminServiceResponse;
import org.project.ttokttok.domain.club.service.mapper.ClubMapper;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory.INTRODUCTION_IMAGE;
import static org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory.PROFILE_IMAGE;

@Service
@RequiredArgsConstructor
public class ClubAdminService {

    private final ClubRepository clubRepository;
    private final ApplyFormRepository applyFormRepository;
    private final ClubMapper mapper;

    private final S3Service s3Service;

    // todo: 나중에 무조건 분할 들어가야 함.
    @Transactional
    public void updateContent(String username, ClubContentUpdateServiceRequest request) {
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        validateAdmin(username, club.getAdmin().getUsername());

        if (hasProfileImage(request)) {
            updateProfileImage(club, request.profileImage().get());
        }

        if (hasApplyFormUpdate(request)) {
            updateApplyForm(club, request);
        }

        mapper.updateClubFromRequest(club, request);
    }

    public String updateMarkdownImage(MarkdownImageUpdateRequest request) {
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        validateAdmin(request.username(), club.getAdmin().getUsername());

        MultipartFile file = request.imageFile();

        validateImage(file.getContentType());

        return s3Service.uploadFile(file, INTRODUCTION_IMAGE.getDirectoryName());
    }

    // 모집 마감, 재시작 토글 로직
    @Transactional
    public void toggleRecruitment(String username, String clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(ClubNotFoundException::new);

        validateAdmin(username, club.getAdmin().getUsername());

        // 현재 존재하는 활성화된 지원 폼을 찾음.
        Optional<ApplyForm> form = applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE);

        if (form.isPresent()) {
            // 활성화된 폼이 존재한다면, 모집 상태를 토글함.
            form.get().updateFormStatus();
        } else if (form.isEmpty()) {
            // 활성화된 폼이 없다면, 가장 최근에 생성된 지원 폼을 찾아 활성화시킴.
            ApplyForm latestForm = applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(clubId)
                    .orElseThrow(ApplyFormNotFoundException::new);

            latestForm.updateFormStatus();
        } else {
            // 지원 폼이 존재하지 않은 경우 예외 처리
            throw new NoApplyFormExistException();
        }
    }

    public ClubDetailAdminServiceResponse getClubContent(String clubId) {
        return null;
    }

    private void validateImage(String contentType) {
        if (contentType == null || !isImage(contentType))
            throw new FileIsNotImageException();
    }

    // todo: 추후 리팩토링
    private boolean isImage(String contentType) {
        return contentType.startsWith("image/jpeg") ||
                contentType.startsWith("image/png") ||
                contentType.startsWith("image/webp");
    }

    // 요청에 프로필 이미지 업데이트 요청이 있는지 확인
    private boolean hasProfileImage(ClubContentUpdateServiceRequest request) {
        return request.profileImage() != null && request.profileImage().isPresent();
    }

    // 프로필 이미지 업데이트 로직
    private void updateProfileImage(Club club, MultipartFile profileImage) {
        String profileImgKey = s3Service.uploadFile(profileImage, PROFILE_IMAGE.getDirectoryName());
        validateProfileImgExist(club, profileImgKey);

        club.setProfileImageUrl(profileImgKey);
    }

    // 요청에 지원 폼 업데이트 요청이 있는지 확인
    private boolean hasApplyFormUpdate(ClubContentUpdateServiceRequest request) {
        return request.applyStartDate().isPresent() || request.applyEndDate().isPresent() ||
                request.grades().isPresent() || request.maxApplyCount().isPresent() ||
                // request.recruiting().isPresent(); - ApplyForm에서 관리하므로 제거
                false;
    }

    // 지원 폼 업데이트 로직
    private void updateApplyForm(Club club, ClubContentUpdateServiceRequest request) {
        ApplyForm applyForm = applyFormRepository.findByClubIdAndStatus(club.getId(), ACTIVE)
                .orElseThrow(ApplyFormNotFoundException::new);

        // 모집 시작일과 종료일이 모두 존재할 경우, 시작일이 종료일보다 이후인지 검증
        if (request.applyStartDate().isPresent() && request.applyEndDate().isPresent()) {
            validateApplyPeriod(request.applyStartDate().get(), request.applyEndDate().get());
        }

        applyForm.updateApplyInfo(
                request.applyStartDate().orElse(null),
                request.applyEndDate().orElse(null),
                request.maxApplyCount().orElse(null),
                request.grades().orElse(null),
                // request.recruiting().orElse(null) - ApplyForm에서 관리
                null
        );
    }

    private void validateApplyPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException();
        }
    }

    // 기존 프로필 이미지가 있다면 삭제
    private void validateProfileImgExist(Club club, String profileImgKey) {
        if (club.getProfileImageUrl() != null && !club.getProfileImageUrl().equals(profileImgKey)) {
            s3Service.deleteFile(club.getProfileImageUrl());
        }
    }

    // 동아리 관리자 검증
    private void validateAdmin(String username, String targetAdminUsername) {
        if (!username.equals(targetAdminUsername))
            throw new NotClubAdminException();
    }

    public String getImageUrl(String imageKey) {
        return s3Service.generatePresignedUrl(imageKey);
    }
}
