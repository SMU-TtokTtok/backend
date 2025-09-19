package org.project.ttokttok.domain.club.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory.PROFILE_IMAGE;

@ExtendWith(MockitoExtension.class)
class ClubAdminServiceTest {

    @InjectMocks
    private ClubAdminService clubAdminService;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private S3Service s3Service;
//
//    private final String ADMIN_USERNAME = "adminUser";
//    private final String CLUB_ID = "club-id";
//
//    private Club createClubWithAdmin() {
//        Admin admin = Admin.builder()
//                .username(ADMIN_USERNAME)
//                .password("encoded-password")
//                .build();
//
//        return Club.builder()
//                .admin(admin)
//                .build();
//    }
//
//    @DisplayName("updateContent(): 정상적으로 수정에 성공한다")
//    @Test
//    void updateContent_success() {
//        // given
//        Club club = createClubWithAdmin();
//
//        ClubContentUpdateServiceRequest request = new ClubContentUpdateServiceRequest(
//                CLUB_ID,
//                JsonNullable.of("new name"),
//                JsonNullable.undefined(),
//                JsonNullable.undefined(),
//                JsonNullable.undefined(),
//                JsonNullable.undefined(),
//                JsonNullable.of("new summary"),
//                JsonNullable.undefined(),
//                JsonNullable.of("new content"),
//                JsonNullable.undefined(),
//                JsonNullable.undefined(),
//                JsonNullable.undefined(),
//                JsonNullable.undefined()
//        );
//
//        given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
//
//        // when
//        clubAdminService.updateContent(ADMIN_USERNAME, request);
//
//        // then
//        // 실제 서비스에서는 club.updateFrom()을 호출하므로 해당 메서드가 호출되었는지 확인할 수 없음
//        // 대신 club이 올바르게 찾아졌는지만 확인
//        verify(clubRepository).findById(CLUB_ID);
//    }
//
//    @DisplayName("updateContent(): 동아리 관리자가 아닐 경우 예외를 던진다")
//    @Test
//    void updateContent_throwException_whenNotClubAdmin() {
//        // given
//        Club club = createClubWithAdmin();
//        String invalidUsername = "notAdmin";
//
//        ClubContentUpdateServiceRequest request = new ClubContentUpdateServiceRequest(
//                CLUB_ID, JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined()
//        );
//
//        given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
//
//        // when & then
//        assertThatThrownBy(() -> clubAdminService.updateContent(invalidUsername, request))
//                .isInstanceOf(NotClubAdminException.class);
//    }
//
//    @DisplayName("updateContent(): S3 업로드에 실패하면 예외를 던진다")
//    @Test
//    void updateContent_throwException_whenS3UploadFails() {
//        // given
//        Club club = createClubWithAdmin();
//
//        MultipartFile image = mock(MultipartFile.class);
//
//        ClubContentUpdateServiceRequest request = new ClubContentUpdateServiceRequest(
//                CLUB_ID,
//                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.of(image),
//                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined()
//        );
//
//        given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
//        given(s3Service.uploadFile(image, PROFILE_IMAGE.getDirectoryName())).willThrow(new RuntimeException("S3 업로드 실패"));
//
//        // when & then
//        assertThatThrownBy(() -> clubAdminService.updateContent(ADMIN_USERNAME, request))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("S3 업로드 실패");
//    }
//
//    @DisplayName("updateContent(): 일부 데이터만 업데이트해도 성공한다")
//    @Test
//    void updateContent_success_whenPartialDataUpdated() {
//        // given
//        Club club = createClubWithAdmin();
//
//        ClubContentUpdateServiceRequest request = new ClubContentUpdateServiceRequest(
//                CLUB_ID,
//                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.of("요약만 수정"), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined()
//        );
//
//        given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
//
//        // when
//        clubAdminService.updateContent(ADMIN_USERNAME, request);
//
//        // then
//        verify(clubRepository).findById(CLUB_ID);
//    }
//
//    @DisplayName("updateContent(): 기존 이미지가 존재할 경우 삭제 후 새로운 이미지 업로드에 성공한다")
//    @Test
//    void updateContent_success_whenOldImageDeletedAndNewImageUploaded() {
//        // given
//        Club club = createClubWithAdmin();
//        club.updateProfileImgUrl("old/image.png");
//
//        MultipartFile image = mock(MultipartFile.class);
//        given(image.getContentType()).willReturn("image/png");
//        given(image.getSize()).willReturn(1024L);
//
//        String newKey = "profile-images/new.png";
//
//        ClubContentUpdateServiceRequest request = new ClubContentUpdateServiceRequest(
//                CLUB_ID,
//                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.of(image),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined()
//        );
//
//        given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
//        given(s3Service.uploadFile(image, PROFILE_IMAGE.getDirectoryName())).willReturn(newKey);
//
//        // when
//        clubAdminService.updateContent(ADMIN_USERNAME, request);
//
//        // then
//        verify(s3Service).deleteFile("old/image.png");
//        verify(s3Service).uploadFile(image, PROFILE_IMAGE.getDirectoryName());
//    }
//
//    @DisplayName("updateContent(): 이미지 크기가 최대 허용값을 초과하면 ImageMaxSizeOverException 발생")
//    @Test
//    void updateContent_throwException_whenImageSizeOver() {
//        // given
//        Club club = createClubWithAdmin();
//
//        MultipartFile image = mock(MultipartFile.class);
//        given(image.getContentType()).willReturn("image/png");
//        given(image.getSize()).willReturn(10 * 1024 * 1024L); // 10MB (초과)
//
//        ClubContentUpdateServiceRequest request = new ClubContentUpdateServiceRequest(
//                CLUB_ID,
//                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.of(image),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined()
//        );
//
//        given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
//        given(s3Service.uploadFile(image, PROFILE_IMAGE.getDirectoryName())).willReturn("some/key.png");
//
//        // when & then
//        assertThatThrownBy(() -> clubAdminService.updateContent(ADMIN_USERNAME, request))
//                .isInstanceOf(IllegalArgumentException.class);
//    }
//
//    @DisplayName("updateContent(): 이미지 타입이 허용되지 않으면 InvalidImageTypeException 발생")
//    @Test
//    void updateContent_throwException_whenInvalidImageType() {
//        // given
//        Club club = createClubWithAdmin();
//
//        MultipartFile image = mock(MultipartFile.class);
//        given(image.getContentType()).willReturn("application/pdf"); // 허용되지 않는 타입
//
//        ClubContentUpdateServiceRequest request = new ClubContentUpdateServiceRequest(
//                CLUB_ID,
//                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.of(image),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined(), JsonNullable.undefined(),
//                JsonNullable.undefined()
//        );
//
//        given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
//        given(s3Service.uploadFile(image, PROFILE_IMAGE.getDirectoryName())).willReturn("some/key.png");
//
//        // when & then
//        assertThatThrownBy(() -> clubAdminService.updateContent(ADMIN_USERNAME, request))
//                .isInstanceOf(IllegalArgumentException.class);
//    }
}
