package org.project.ttokttok.domain.club.service;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory.INTRODUCTION_IMAGE;
import static org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory.PROFILE_IMAGE;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.FileIsNotImageException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;
import org.project.ttokttok.domain.club.service.dto.request.MarkdownImageUpdateRequest;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailAdminServiceResponse;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.firebase.service.dto.FCMRequest;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubAdminService {

    private final ClubRepository clubRepository;
    private final ApplyFormRepository applyFormRepository;
    private final FCMTokenRepository fcmTokenRepository;

    private final S3Service s3Service;
    private final FCMService fcmService;

    @Transactional
    public void updateContent(String username,
                              String clubId,
                              ClubContentUpdateServiceRequest request,
                              Optional<MultipartFile> profileImage) {

        Club club = validateClubAdmin(username);

        if (hasProfileImage(profileImage)) {
            updateProfileImage(club, profileImage.get());
        }

        if (isRequestNotNull(request)) {
            updateFormSafety(request, club);
            club.updateFrom(request.toClubPatchRequest());
        }
    }

    @Transactional
    public String updateMarkdownImage(String username, MarkdownImageUpdateRequest request) {
        validateClubAdmin(username);

        MultipartFile file = request.imageFile();

        validateImage(file.getContentType());

        return s3Service.uploadFile(file, INTRODUCTION_IMAGE.getDirectoryName());
    }

    @Transactional
    public void toggleRecruitment(String username, String clubId) {
        Club club = validateClubAdmin(username);

        Optional<ApplyForm> form = applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE);

        if (form.isPresent()) {
            boolean wasRecruiting = form.get().isRecruiting();
            form.get().toggleRecruiting();

            log.info("Current apply form status toggled for club: {}, status: {}", clubId, form.get().getStatus());

            if (!wasRecruiting && form.get().isRecruiting()) {
                sendRecruitmentNotification(form.get(), club);
            }
        } else {
            ApplyForm latestForm = applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(clubId)
                    .orElseThrow(ApplyFormNotFoundException::new);

            log.info("No active apply form found for club: {}, activating latest form: {}", clubId, latestForm.getId());
            latestForm.updateFormStatus();

            if (latestForm.isRecruiting()) {
                sendRecruitmentNotification(latestForm, club);
            }
        }
    }

    public ClubDetailAdminServiceResponse getClubContent(String clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new ClubNotFoundException();
        }

        return ClubDetailAdminServiceResponse.from(
                clubRepository.getAdminClubIntro(clubId)
        );
    }

    private void validateImage(String contentType) {
        if (contentType == null || !isImage(contentType)) {
            throw new FileIsNotImageException();
        }
    }

    private boolean isImage(String contentType) {
        return contentType.startsWith("image/jpeg") ||
                contentType.startsWith("image/png") ||
                contentType.startsWith("image/webp");
    }

    private boolean hasProfileImage(Optional<MultipartFile> profileImage) {
        return profileImage.isPresent();
    }

    private void updateProfileImage(Club club, MultipartFile profileImage) {
        String profileImgKey = s3Service.uploadFile(profileImage, PROFILE_IMAGE.getDirectoryName());
        validateProfileImgExist(club, profileImgKey);

        club.updateProfileImgUrl(profileImgKey);
    }

    private boolean hasApplyFormUpdate(ClubContentUpdateServiceRequest request) {
        return request.applyStartDate().isPresent() || request.applyEndDate().isPresent() ||
                request.grades().isPresent() || request.maxApplyCount().isPresent();
    }

    private void updateApplyForm(Club club, ClubContentUpdateServiceRequest request) {
        ApplyForm applyForm = applyFormRepository.findByClubIdAndStatus(club.getId(), ACTIVE)
                .orElseThrow(ApplyFormNotFoundException::new);

        Optional<LocalDate> startDate = request.applyStartDate().isPresent() ?
                Optional.of(request.applyStartDate().get()) : Optional.empty();

        Optional<LocalDate> endDate = request.applyEndDate().isPresent() ?
                Optional.of(request.applyEndDate().get()) : Optional.empty();

        Optional<Integer> maxCount = request.maxApplyCount().isPresent() ?
                Optional.of(request.maxApplyCount().get()) : Optional.empty();

        Optional<Set<ApplicableGrade>> grades = request.grades().isPresent() ?
                Optional.of(request.grades().get()) : Optional.empty();

        applyForm.updateApplyInfo(
                startDate.orElse(null),
                endDate.orElse(null),
                maxCount.orElse(null),
                grades.orElse(null)
        );
    }

    private void validateProfileImgExist(Club club, String profileImgKey) {
        if (club.getProfileImageUrl() != null && !club.getProfileImageUrl().equals(profileImgKey)) {
            s3Service.deleteFile(club.getProfileImageUrl());
        }
    }

    private boolean isRequestNotNull(ClubContentUpdateServiceRequest request) {
        return request != null;
    }

    private void updateFormSafety(ClubContentUpdateServiceRequest request, Club club) {
        if (hasApplyFormUpdate(request)) {
            updateApplyForm(club, request);
        }
    }

    private Club validateClubAdmin(String username) {
        return clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);
    }

    private void sendRecruitmentNotification(ApplyForm applyForm, Club club) {
        List<String> fcmTokens = fcmTokenRepository.findTokensByClubId(club.getId());

        if (fcmTokens.isEmpty()) {
            log.info("No FCM tokens found for applicants of apply form: {}", applyForm.getId());
            return;
        }

        String title = "📢 모집 재개 알림";
        String body = String.format("%s 동아리의 모집이 시작되었습니다! 지금 바로 확인해보세요.", club.getName());

        FCMRequest fcmRequest = FCMRequest.builder()
                .tokens(fcmTokens)
                .title(title)
                .body(body)
                .build();
        fcmService.sendNotification(fcmRequest);

        log.info("FCM notification sent for club: {}, apply form: {}, token count: {}",
                club.getId(), applyForm.getId(), fcmTokens.size());
    }
}
