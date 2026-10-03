package org.project.ttokttok.domain.club.service.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;

class ClubAccessPolicyTest {

    private final ClubAccessPolicy clubAccessPolicy = new ClubAccessPolicy();
    private static final String USERNAME = "clubadmin1";

    @Test
    @DisplayName("동아리 관리자인 경우 접근을 허용한다")
    void validateClubAndAdmin_admin_success() {
        // given
        Club club = mock(Club.class);
        given(club.isManagedBy(USERNAME)).willReturn(true);

        // when & then
        assertThatCode(() -> clubAccessPolicy.validateAdmin(club, USERNAME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동아리 관리자가 아니면 NotClubAdminException이 발생한다")
    void validateClubAndAdmin_notAdmin_forbidden() {
        // given
        Club club = mock(Club.class);
        given(club.isManagedBy(USERNAME)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> clubAccessPolicy.validateAdmin(club, USERNAME))
                .isInstanceOf(NotClubAdminException.class);
    }

    @Test
    @DisplayName("요청한 동아리 ID가 대상 동아리 ID와 같으면 접근을 허용한다")
    void validateSameClubId_sameId_success() {
        assertThatCode(() -> clubAccessPolicy.validateSameClubId("club-a", "club-a"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("요청한 동아리 ID가 대상 동아리 ID와 다르면 NotClubAdminException이 발생한다")
    void validateSameClubId_differentId_forbidden() {
        assertThatThrownBy(() -> clubAccessPolicy.validateSameClubId("club-a", "club-b"))
                .isInstanceOf(NotClubAdminException.class);
    }
}
