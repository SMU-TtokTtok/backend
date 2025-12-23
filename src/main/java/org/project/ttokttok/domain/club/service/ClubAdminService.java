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
import org.project.ttokttok.domain.favorite.repository.FavoriteRepository;
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

    // todo: 나중에 무조건 분할 들어가야 함.
    @Transactional
    public void updateContent(String username,
                              String clubId,
                              ClubContentUpdateServiceRequest request,
                              Optional<MultipartFile> profileImage) {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(ClubNotFoundException::new);

        validateAdmin(username, club.getAdmin().getUsername());

        if (hasProfileImage(profileImage)) {
            updateProfileImage(club, profileImage.get());
        }

        if (isRequestNotNull(request)) {
            updateFormSafety(request, club);
            club.updateFrom(request.toClubPatchRequest());
        }
    }

    @Transactional
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
            boolean wasRecruiting = form.get().isRecruiting();
            form.get().toggleRecruiting();

            log.info("Current apply form status toggled for club: {}, status: {}", clubId, form.get().getStatus());

            // 모집이 다시 시작된 경우 FCM 알림 전송
            if (!wasRecruiting && form.get().isRecruiting()) {
                sendRecruitmentNotification(form.get(), club);
            }
        } else {
            // 활성화된 폼이 없다면, 가장 최근에 생성된 지원 폼을 찾아 활성화시킴.
            ApplyForm latestForm = applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(clubId)
                    .orElseThrow(ApplyFormNotFoundException::new);

            log.info("No active apply form found for club: {}, activating latest form: {}", clubId, latestForm.getId());
            latestForm.updateFormStatus();

            // 활성화된 폼이 모집 중이라면 FCM 알림 전송
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

    // todo: 추후 리팩토링
    private boolean isImage(String contentType) {
        return contentType.startsWith("image/jpeg") ||
                contentType.startsWith("image/png") ||
                contentType.startsWith("image/webp");
    }

    // 요청에 프로필 이미지 업데이트 요청이 있는지 확인
    private boolean hasProfileImage(Optional<MultipartFile> profileImage) {
        return profileImage.isPresent();
    }

    // 프로필 이미지 업데이트 로직
    private void updateProfileImage(Club club, MultipartFile profileImage) {
        String profileImgKey = s3Service.uploadFile(profileImage, PROFILE_IMAGE.getDirectoryName());
        validateProfileImgExist(club, profileImgKey);

        club.updateProfileImgUrl(profileImgKey);
    }

    // 요청에 지원 폼 업데이트 요청이 있는지 확인
    private boolean hasApplyFormUpdate(ClubContentUpdateServiceRequest request) {
        return request.applyStartDate().isPresent() || request.applyEndDate().isPresent() ||
                request.grades().isPresent() || request.maxApplyCount().isPresent();
    }

    // TODO: 추후 날짜 정합성 관련 로직 추가.
    // 지원 폼 업데이트 로직
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

    // 기존 프로필 이미지가 있다면 삭제
    private void validateProfileImgExist(Club club, String profileImgKey) {
        if (club.getProfileImageUrl() != null && !club.getProfileImageUrl().equals(profileImgKey)) {
            s3Service.deleteFile(club.getProfileImageUrl());
        }
    }

    // 요청이 null이 아니고, 지원 폼 업데이트 요청이 있는지 확인
    private boolean isRequestNotNull(ClubContentUpdateServiceRequest request) {
        return request != null;
    }

    private void updateFormSafety(ClubContentUpdateServiceRequest request, Club club) {
        if (hasApplyFormUpdate(request)) {
            updateApplyForm(club, request);
        }
    }

    // 동아리 관리자 검증
    private void validateAdmin(String username, String targetAdminUsername) {
        if (!username.equals(targetAdminUsername)) {
            throw new NotClubAdminException();
        }
    }

    // 모집 재개 시 지원자들에게 FCM 알림 전송
    private void sendRecruitmentNotification(ApplyForm applyForm, Club club) {
        // 해당 지원폼에 지원한 지원자들의 이메일 목록 조회
        List<String> fcmTokens = fcmTokenRepository.findTokensByClubId(club.getId());

        if (fcmTokens.isEmpty()) {
            log.info("No FCM tokens found for applicants of apply form: {}", applyForm.getId());
            return;
        }

        // FCM 알림 메시지 생성 및 전송
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
