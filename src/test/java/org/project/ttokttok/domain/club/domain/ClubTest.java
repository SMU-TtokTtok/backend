package org.project.ttokttok.domain.club.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.service.dto.request.ClubPatchRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClubTest {

    private Club createClub() {
        Admin admin = mock(Admin.class);
        return Club.builder()
                .admin(admin)
                .clubName("테스트 동아리")
                .clubUniv(ClubUniv.ENGINEERING)
                .build();
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("동아리를 생성하면 기본값들이 설정된다.")
        void createSuccess() {
            Club club = createClub();

            assertThat(club.getName()).isEqualTo("테스트 동아리");
            assertThat(club.getClubUniv()).isEqualTo(ClubUniv.ENGINEERING);
            assertThat(club.getProfileImageUrl()).isNull();
            assertThat(club.getSummary()).isEqualTo("아직 동아리 소개가 없어요 🙂");
            assertThat(club.getClubType()).isEqualTo(ClubType.CENTRAL);
            assertThat(club.getClubCategory()).isEqualTo(ClubCategory.ACADEMIC);
            assertThat(club.getCustomCategory()).isEqualTo("");
            assertThat(club.getContent()).isEqualTo("동아리 소개가 아직 작성되지 않았어요!");
            assertThat(club.getViewCount()).isZero();
        }
    }

    @Nested
    @DisplayName("updateProfileImgUrl()")
    class UpdateProfileImgUrl {

        @Test
        @DisplayName("프로필 이미지 URL을 변경할 수 있다.")
        void updatesUrl() {
            Club club = createClub();

            club.updateProfileImgUrl("https://example.com/image.png");

            assertThat(club.getProfileImageUrl()).isEqualTo("https://example.com/image.png");
        }
    }

    @Nested
    @DisplayName("updateFrom()")
    class UpdateFrom {

        @Test
        @DisplayName("값이 채워진 필드만 부분 수정된다.")
        void partialUpdate() {
            Club club = createClub();

            ClubPatchRequest request = new ClubPatchRequest(
                    JsonNullable.of("새 이름"),
                    JsonNullable.undefined(),
                    JsonNullable.of(ClubCategory.SPORTS),
                    JsonNullable.undefined(),
                    JsonNullable.undefined(),
                    JsonNullable.undefined(),
                    JsonNullable.undefined()
            );

            club.updateFrom(request);

            assertThat(club.getName()).isEqualTo("새 이름");
            assertThat(club.getClubCategory()).isEqualTo(ClubCategory.SPORTS);
            // 전달되지 않은 필드는 기존 값 유지
            assertThat(club.getClubType()).isEqualTo(ClubType.CENTRAL);
            assertThat(club.getSummary()).isEqualTo("아직 동아리 소개가 없어요 🙂");
        }

        @Test
        @DisplayName("모든 필드가 채워지면 전체가 수정된다.")
        void updatesAllFields() {
            Club club = createClub();

            ClubPatchRequest request = new ClubPatchRequest(
                    JsonNullable.of("전체수정동아리"),
                    JsonNullable.of(ClubType.UNION),
                    JsonNullable.of(ClubCategory.CULTURE),
                    JsonNullable.of(ClubUniv.DESIGN),
                    JsonNullable.of("커스텀분류"),
                    JsonNullable.of("새 한줄소개"),
                    JsonNullable.of("새 소개 내용")
            );

            club.updateFrom(request);

            assertThat(club.getName()).isEqualTo("전체수정동아리");
            assertThat(club.getClubType()).isEqualTo(ClubType.UNION);
            assertThat(club.getClubCategory()).isEqualTo(ClubCategory.CULTURE);
            assertThat(club.getClubUniv()).isEqualTo(ClubUniv.DESIGN);
            assertThat(club.getCustomCategory()).isEqualTo("커스텀분류");
            assertThat(club.getSummary()).isEqualTo("새 한줄소개");
            assertThat(club.getContent()).isEqualTo("새 소개 내용");
        }
    }
}
