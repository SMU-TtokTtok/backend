package org.project.ttokttok.domain.club.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.FileIsNotImageException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.request.MarkdownImageUpdateRequest;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.firebase.service.dto.FCMRequest;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubAdminService - 동아리 관리 동작")
class ClubAdminServiceBehaviorTest {

    @Mock private ClubRepository clubRepository;
    @Mock private ApplyFormRepository applyFormRepository;
    @Mock private FCMTokenRepository fcmTokenRepository;
    @Mock private S3Service s3Service;
    @Mock private FCMService fcmService;

    @InjectMocks private ClubAdminService clubAdminService;

    private static final String USERNAME = "admin@sangmyung.kr";
    private static final String CLUB_ID = "club-1";

    @Nested
    @DisplayName("updateMarkdownImage()")
    class UpdateMarkdownImage {

        @Test
        @DisplayName("이미지 파일이면 S3에 업로드하고 키를 반환한다")
        void success() {
            Club club = mock(Club.class);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            MultipartFile file = mock(MultipartFile.class);
            given(file.getContentType()).willReturn("image/png");
            given(s3Service.uploadFile(any(), anyString())).willReturn("s3-key");

            String result = clubAdminService.updateMarkdownImage(
                    USERNAME, MarkdownImageUpdateRequest.of(USERNAME, CLUB_ID, file));

            org.assertj.core.api.Assertions.assertThat(result).isEqualTo("s3-key");
        }

        @Test
        @DisplayName("이미지가 아닌 파일이면 FileIsNotImageException을 던진다")
        void throwsWhenNotImage() {
            Club club = mock(Club.class);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            MultipartFile file = mock(MultipartFile.class);
            given(file.getContentType()).willReturn("application/pdf");

            assertThatThrownBy(() -> clubAdminService.updateMarkdownImage(
                    USERNAME, MarkdownImageUpdateRequest.of(USERNAME, CLUB_ID, file)))
                    .isInstanceOf(FileIsNotImageException.class);
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 NotClubAdminException을 던진다")
        void throwsWhenNotAdmin() {
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());
            MultipartFile file = mock(MultipartFile.class);

            assertThatThrownBy(() -> clubAdminService.updateMarkdownImage(
                    USERNAME, MarkdownImageUpdateRequest.of(USERNAME, CLUB_ID, file)))
                    .isInstanceOf(NotClubAdminException.class);
        }
    }

    @Nested
    @DisplayName("getClubContent()")
    class GetClubContent {

        @Test
        @DisplayName("존재하지 않는 동아리면 ClubNotFoundException을 던진다")
        void throwsWhenNotFound() {
            given(clubRepository.existsById(CLUB_ID)).willReturn(false);

            assertThatThrownBy(() -> clubAdminService.getClubContent(CLUB_ID))
                    .isInstanceOf(ClubNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateContent()")
    class UpdateContent {

        @Test
        @DisplayName("프로필 이미지가 있으면 S3 업로드 후 URL을 갱신한다")
        void updatesProfileImage() {
            Club club = mock(Club.class);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(club.getProfileImageUrl()).willReturn(null);
            MultipartFile profile = mock(MultipartFile.class);
            given(s3Service.uploadFile(any(), anyString())).willReturn("new-key");

            clubAdminService.updateContent(USERNAME, CLUB_ID, null, Optional.of(profile));

            verify(club).updateProfileImgUrl("new-key");
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 NotClubAdminException을 던진다")
        void throwsWhenNotAdmin() {
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    clubAdminService.updateContent(USERNAME, CLUB_ID, null, Optional.empty()))
                    .isInstanceOf(NotClubAdminException.class);
        }
    }

    @Nested
    @DisplayName("toggleRecruitment()")
    class ToggleRecruitment {

        @Test
        @DisplayName("모집중이 아니던 활성 폼을 켜면 모집 재개 알림을 발송한다")
        void togglesAndSendsNotificationWhenStartRecruiting() {
            Club club = mock(Club.class);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(club.getId()).willReturn(CLUB_ID);
            given(club.getName()).willReturn("떡떡동아리");

            ApplyForm form = mock(ApplyForm.class);
            given(applyFormRepository.findByClubIdAndStatus(any(), any())).willReturn(Optional.of(form));
            given(form.isRecruiting()).willReturn(false, true); // 토글 전 false, 토글 후 true
            given(fcmTokenRepository.findTokensByClubId(CLUB_ID)).willReturn(List.of("token-1"));

            clubAdminService.toggleRecruitment(USERNAME, CLUB_ID);

            verify(form).toggleRecruiting();
            verify(fcmService).sendNotification(any(FCMRequest.class));
        }

        @Test
        @DisplayName("이미 모집중이던 폼을 끄면 알림을 보내지 않는다")
        void togglesOffWithoutNotification() {
            Club club = mock(Club.class);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            ApplyForm form = mock(ApplyForm.class);
            given(applyFormRepository.findByClubIdAndStatus(any(), any())).willReturn(Optional.of(form));
            given(form.isRecruiting()).willReturn(true, false);

            clubAdminService.toggleRecruitment(USERNAME, CLUB_ID);

            verify(form).toggleRecruiting();
            verify(fcmService, never()).sendNotification(any());
        }

        @Test
        @DisplayName("활성 폼이 없으면 최신 폼을 활성화한다")
        void activatesLatestWhenNoActiveForm() {
            Club club = mock(Club.class);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applyFormRepository.findByClubIdAndStatus(any(), any())).willReturn(Optional.empty());

            ApplyForm latest = mock(ApplyForm.class);
            given(applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(CLUB_ID))
                    .willReturn(Optional.of(latest));
            given(latest.isRecruiting()).willReturn(false);

            clubAdminService.toggleRecruitment(USERNAME, CLUB_ID);

            verify(latest).updateFormStatus();
        }

        @Test
        @DisplayName("활성 폼도 없고 최신 폼도 없으면 ApplyFormNotFoundException을 던진다")
        void throwsWhenNoFormAtAll() {
            Club club = mock(Club.class);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applyFormRepository.findByClubIdAndStatus(any(), any())).willReturn(Optional.empty());
            given(applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(CLUB_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> clubAdminService.toggleRecruitment(USERNAME, CLUB_ID))
                    .isInstanceOf(ApplyFormNotFoundException.class);
        }
    }
}
