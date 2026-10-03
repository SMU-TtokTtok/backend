package org.project.ttokttok.domain.club.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.service.policy.ClubAccessPolicy;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.firebase.service.dto.FCMRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.INACTIVE;
import static org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory.PROFILE_IMAGE;

@ExtendWith(MockitoExtension.class)
class ClubAdminServiceTest {

    @InjectMocks
    private ClubAdminService clubAdminService;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private ApplyFormRepository applyFormRepository;

    @Mock
    private FCMTokenRepository fcmTokenRepository;

    @Mock
    private FCMService fcmService;

    @Spy
    private ClubAccessPolicy clubAccessPolicy;

    @Nested
    @DisplayName("updateContent(): 동아리 내용 수정")
    class UpdateContentTest {

        @ParameterizedTest(name = "프로필 이미지 첨부={0}")
        @ValueSource(booleans = {false, true})
        @DisplayName("자기 동아리 내용을 수정하고 이미지가 있으면 기존 이미지를 교체한다")
        void updateContent_success(boolean hasProfileImage) {
            // given
            Club club = Club.builder().admin(mock(Admin.class)).clubName("기존 동아리").build();
            ReflectionTestUtils.setField(club, "id", "club-id");
            club.updateProfileImgUrl("old/profile.png");
            given(clubRepository.findByAdminUsername("adminUser")).willReturn(Optional.of(club));
            ClubContentUpdateServiceRequest request = ClubContentUpdateServiceRequest.builder()
                    .name(JsonNullable.of("변경된 동아리"))
                    .summary(JsonNullable.of("변경된 소개"))
                    .content(JsonNullable.of("변경된 내용"))
                    .applyStartDate(JsonNullable.undefined())
                    .applyEndDate(JsonNullable.undefined())
                    .grades(JsonNullable.undefined())
                    .maxApplyCount(JsonNullable.undefined())
                    .build();
            Optional<MultipartFile> profileImage = Optional.empty();
            if (hasProfileImage) {
                MultipartFile image = mock(MultipartFile.class);
                profileImage = Optional.of(image);
                given(s3Service.uploadFile(image, PROFILE_IMAGE.getDirectoryName()))
                        .willReturn("new/profile.png");
            }

            // when
            clubAdminService.updateContent("adminUser", "club-id", request, profileImage);

            // then
            assertThat(club.getName()).isEqualTo("변경된 동아리");
            assertThat(club.getSummary()).isEqualTo("변경된 소개");
            assertThat(club.getContent()).isEqualTo("변경된 내용");
            if (hasProfileImage) {
                assertThat(club.getProfileImageUrl()).isEqualTo("new/profile.png");
                verify(s3Service).uploadFile(profileImage.orElseThrow(), PROFILE_IMAGE.getDirectoryName());
                verify(s3Service).deleteFile("old/profile.png");
            } else {
                assertThat(club.getProfileImageUrl()).isEqualTo("old/profile.png");
                verifyNoInteractions(s3Service);
            }
            verifyNoInteractions(applyFormRepository, fcmTokenRepository, fcmService);
        }

        @ParameterizedTest(name = "프로필 이미지 첨부={0}")
        @ValueSource(booleans = {false, true})
        @DisplayName("다른 동아리 ID를 전달하면 내용과 이미지를 변경하지 않고 거부한다")
        void updateContent_otherClub_forbidden(boolean hasProfileImage) {
            // given
            String username = "adminUser";
            Club club = Club.builder().admin(mock(Admin.class)).clubName("기존 동아리").build();
            ReflectionTestUtils.setField(club, "id", "club-id");
            club.updateProfileImgUrl("old/profile.png");
            String originalContent = club.getContent();
            String originalSummary = club.getSummary();
            given(clubRepository.findByAdminUsername(username)).willReturn(Optional.of(club));

            ClubContentUpdateServiceRequest request = ClubContentUpdateServiceRequest.builder()
                    .name(JsonNullable.of("변경된 동아리"))
                    .summary(JsonNullable.of("변경된 소개"))
                    .content(JsonNullable.of("변경된 내용"))
                    .build();
            Optional<MultipartFile> profileImage = hasProfileImage
                    ? Optional.of(mock(MultipartFile.class)) : Optional.empty();

            // when & then
            assertThatThrownBy(() -> clubAdminService.updateContent(
                    username, "other-club-id", request, profileImage))
                    .isInstanceOf(NotClubAdminException.class);
            assertThat(club.getName()).isEqualTo("기존 동아리");
            assertThat(club.getSummary()).isEqualTo(originalSummary);
            assertThat(club.getContent()).isEqualTo(originalContent);
            assertThat(club.getProfileImageUrl()).isEqualTo("old/profile.png");
            verifyNoInteractions(s3Service, applyFormRepository, fcmTokenRepository, fcmService);
        }
    }

    @Nested
    @DisplayName("toggleRecruitment(): 모집 상태 변경")
    class ToggleRecruitmentTest {

        private static final String USERNAME = "adminUser";
        private static final String CLUB_ID = "club-id";
        private static final String OTHER_CLUB_ID = "other-club-id";

        private Club createClub() {
            Club club = Club.builder().admin(mock(Admin.class)).clubName("테스트 동아리").build();
            ReflectionTestUtils.setField(club, "id", CLUB_ID);
            return club;
        }

        private ApplyForm createApplyForm(Club club, boolean recruiting) {
            ApplyForm form = ApplyForm.builder().club(club).build();
            ReflectionTestUtils.setField(form, "isRecruiting", recruiting);
            return form;
        }

        private void verifyRecruitmentNotification(List<String> tokens) {
            ArgumentCaptor<FCMRequest> captor = ArgumentCaptor.forClass(FCMRequest.class);
            verify(fcmService).sendNotification(captor.capture());
            assertThat(captor.getValue().tokens()).containsExactlyElementsOf(tokens);
            assertThat(captor.getValue().title()).isEqualTo("📢 모집 재개 알림");
            assertThat(captor.getValue().body()).contains("테스트 동아리");
        }

        @ParameterizedTest(name = "알림 대상 존재={0}")
        @ValueSource(booleans = {false, true})
        @DisplayName("활성 지원폼의 모집을 시작하고 알림 대상이 있을 때만 FCM을 호출한다")
        void toggleRecruitment_startsRecruiting(boolean hasTokens) {
            // given
            Club club = createClub();
            ApplyForm form = createApplyForm(club, false);
            List<String> tokens = hasTokens ? List.of("token-1", "token-2") : List.of();
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(form));
            given(fcmTokenRepository.findTokensByClubId(CLUB_ID)).willReturn(tokens);

            // when
            clubAdminService.toggleRecruitment(USERNAME, CLUB_ID);

            // then
            assertThat(form.isRecruiting()).isTrue();
            verify(applyFormRepository, never()).findTopByClubIdOrderByCreatedAtDesc(CLUB_ID);
            verify(fcmTokenRepository).findTokensByClubId(CLUB_ID);
            if (hasTokens) {
                verifyRecruitmentNotification(tokens);
            } else {
                verifyNoInteractions(fcmService);
            }
        }

        @ParameterizedTest(name = "최신 지원폼 모집 상태={0}")
        @ValueSource(booleans = {false, true})
        @DisplayName("활성 지원폼이 없으면 최신 지원폼을 활성화하고 모집 중일 때만 알림을 보낸다")
        void toggleRecruitment_activatesLatestForm(boolean recruiting) {
            // given
            Club club = createClub();
            ApplyForm form = createApplyForm(club, recruiting);
            ReflectionTestUtils.setField(form, "status", INACTIVE);
            List<String> tokens = List.of("token-1");
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.empty());
            given(applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(CLUB_ID))
                    .willReturn(Optional.of(form));
            if (recruiting) {
                given(fcmTokenRepository.findTokensByClubId(CLUB_ID)).willReturn(tokens);
            }

            // when
            clubAdminService.toggleRecruitment(USERNAME, CLUB_ID);

            // then
            assertThat(form.getStatus()).isEqualTo(ACTIVE);
            assertThat(form.isRecruiting()).isEqualTo(recruiting);
            if (recruiting) {
                verifyRecruitmentNotification(tokens);
            } else {
                verifyNoInteractions(fcmTokenRepository, fcmService);
            }
        }

        @Test
        @DisplayName("자기 동아리의 활성 지원폼이 모집 중이면 모집을 종료하고 알림을 보내지 않는다")
        void toggleRecruitment_stopsRecruiting() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(createClub()));
            ApplyForm applyForm = createApplyForm(createClub(), true);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE))
                    .willReturn(Optional.of(applyForm));

            // when
            clubAdminService.toggleRecruitment(USERNAME, CLUB_ID);

            // then
            assertThat(applyForm.isRecruiting()).isFalse();
            verifyNoInteractions(fcmTokenRepository, fcmService);
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 조회와 알림 없이 거부한다")
        void toggleRecruitment_notAdmin_forbidden() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clubAdminService.toggleRecruitment(USERNAME, CLUB_ID))
                    .isInstanceOf(NotClubAdminException.class);
            verifyNoInteractions(applyFormRepository, fcmTokenRepository, fcmService);
        }

        @Test
        @DisplayName("다른 동아리의 모집 상태 변경은 지원폼 조회와 알림 없이 거부한다")
        void toggleRecruitment_otherClub_forbidden() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME))
                    .willReturn(Optional.of(createClub()));

            // when & then
            assertThatThrownBy(() -> clubAdminService.toggleRecruitment(USERNAME, OTHER_CLUB_ID))
                    .isInstanceOf(NotClubAdminException.class);
            verifyNoInteractions(applyFormRepository, fcmTokenRepository, fcmService);
        }

        @Test
        @DisplayName("활성 지원폼과 최신 지원폼이 모두 없으면 예외가 발생하고 알림을 보내지 않는다")
        void toggleRecruitment_noApplyForm() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(createClub()));
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.empty());
            given(applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(CLUB_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clubAdminService.toggleRecruitment(USERNAME, CLUB_ID))
                    .isInstanceOf(ApplyFormNotFoundException.class);
            verifyNoInteractions(fcmTokenRepository, fcmService);
        }
    }
}
