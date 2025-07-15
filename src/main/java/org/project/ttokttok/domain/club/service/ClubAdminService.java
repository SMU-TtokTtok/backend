package org.project.ttokttok.domain.club.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.exception.InvalidDateRangeException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.ImageMaxSizeOverException;
import org.project.ttokttok.domain.club.exception.InvalidImageTypeException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;
import org.project.ttokttok.domain.club.service.mapper.ClubMapper;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Set;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

@Service
@RequiredArgsConstructor
public class ClubAdminService {

    private final ClubRepository clubRepository;
    private final ApplyFormRepository applyFormRepository;
    private final ClubMapper mapper;

    private final S3Service s3Service;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L; // 5MB

    private static final String PROFILE_IMAGE_DIR = "profile-images/";

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

    // 요청에 프로필 이미지 업데이트 요청이 있는지 확인
    private boolean hasProfileImage(ClubContentUpdateServiceRequest request) {
        return request.profileImage() != null && request.profileImage().isPresent();
    }

    // 프로필 이미지 업데이트 로직
    private void updateProfileImage(Club club, MultipartFile profileImage) {
        String profileImgKey = s3Service.uploadFile(profileImage, PROFILE_IMAGE_DIR);
        validateImageContentType(profileImage);
        validateImageSize(profileImage);
        validateProfileImgExist(club, profileImgKey);

        club.setProfileImageUrl(profileImgKey);
    }

    // 요청에 지원 폼 업데이트 요청이 있는지 확인
    private boolean hasApplyFormUpdate(ClubContentUpdateServiceRequest request) {
        return request.applyStartDate().isPresent() || request.applyEndDate().isPresent() ||
                request.grades().isPresent() || request.maxApplyCount().isPresent() ||
                request.recruiting().isPresent();
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
                request.recruiting().orElse(null)
        );
    }

    private void validateApplyPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException();
        }
    }

    // 이미지가 유효한 지 검증
    private void validateImageContentType(MultipartFile profileImage) {
        if (!ALLOWED_CONTENT_TYPES.contains(profileImage.getContentType())) {
            throw new InvalidImageTypeException();
        }
    }

    // 이미지 크기가 유효한지 검증
    private void validateImageSize(MultipartFile profileImage) {
        if (profileImage.getSize() > MAX_IMAGE_SIZE) {
            throw new ImageMaxSizeOverException();
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
}
