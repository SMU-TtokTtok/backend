package org.project.ttokttok.domain.favorite.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FavoriteTest {

    @Test
    @DisplayName("create()로 즐겨찾기를 생성하면 사용자와 동아리가 설정된다.")
    void createSuccess() {
        User user = mock(User.class);
        Club club = mock(Club.class);

        Favorite favorite = Favorite.create(user, club);

        assertThat(favorite.getUser()).isEqualTo(user);
        assertThat(favorite.getClub()).isEqualTo(club);
        assertThat(favorite.getId()).isNull(); // JPA 저장 전이므로 null
    }

    @Test
    @DisplayName("서로 다른 사용자/동아리로 생성하면 각각 독립적인 즐겨찾기가 만들어진다.")
    void createIndependentFavorites() {
        User user1 = mock(User.class);
        User user2 = mock(User.class);
        Club club = mock(Club.class);

        Favorite favorite1 = Favorite.create(user1, club);
        Favorite favorite2 = Favorite.create(user2, club);

        assertThat(favorite1.getUser()).isEqualTo(user1);
        assertThat(favorite2.getUser()).isEqualTo(user2);
        assertThat(favorite1.getClub()).isEqualTo(favorite2.getClub());
    }
}
