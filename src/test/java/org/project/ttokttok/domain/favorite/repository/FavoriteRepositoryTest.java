package org.project.ttokttok.domain.favorite.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteListServiceRequest;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FavoriteRepositoryTest implements RepositoryTestSupport {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ClubRepository clubRepository;

    private User user;
    private Club club;

    @BeforeEach
    void setUp() {
        Admin admin = createAdmin("adminuser", "admin@sangmyung.kr");
        user = createUser("test@sangmyung.kr");
        club = createClub(admin, "test club");
    }

    @AfterEach
    void tearDown() {
        favoriteRepository.deleteAll();
        clubRepository.deleteAll();
        adminRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자 이메일과 동아리 ID로 즐겨찾기를 조회한다.")
    void findByUserEmailAndClubId() {
        // given
        Favorite favorite = Favorite.builder()
                .user(user)
                .club(club)
                .build();
        
        favoriteRepository.save(favorite);

        // when
        Optional<Favorite> result = favoriteRepository.findByUserEmailAndClubId(user.getEmail(), club.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getEmail()).isEqualTo(user.getEmail());
        assertThat(result.get().getClub().getId()).isEqualTo(club.getId());
    }

    @Test
    @DisplayName("사용자 이메일과 동아리 ID로 즐겨찾기 존재 여부를 확인한다.")
    void existsByUserEmailAndClubId() {
        // given
        Favorite favorite = Favorite.builder()
                .user(user)
                .club(club)
                .build();
        
        favoriteRepository.save(favorite);

        // when
        boolean exists = favoriteRepository.existsByUserEmailAndClubId(user.getEmail(), club.getId());
        boolean notExists = favoriteRepository.existsByUserEmailAndClubId(user.getEmail(), "non-existent-id");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("동아리 ID로 해당 동아리의 즐겨찾기 총 개수를 조회한다.")
    void countByClubId() {
        // given
        User user2 = createUser("user2@sangmyung.kr");
        
        Favorite favorite1 = Favorite.builder().user(user).club(club).build();
        Favorite favorite2 = Favorite.builder().user(user2).club(club).build();
        
        favoriteRepository.save(favorite1);
        favoriteRepository.save(favorite2);

        // when
        long count = favoriteRepository.countByClubId(club.getId());

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("사용자의 즐겨찾기 목록을 페이징 및 정렬하여 조회한다.")
    void findFavoritesByRequest() throws InterruptedException {
        // given
        Admin admin1 = createAdmin("adminuser1", "admin1@sangmyung.kr");
        Admin admin2 = createAdmin("adminuser2", "admin2@sangmyung.kr");
        Admin admin3 = createAdmin("adminuser3", "admin3@sangmyung.kr");
        
        Club club1 = createClub(admin1, "unique_club_1");
        Club club2 = createClub(admin2, "unique_club_2");
        Club club3 = createClub(admin3, "unique_club_3");
        
        // 생성 시간 순서를 보장하기 위해 save 사이에 지연 추가
        Favorite favorite1 = Favorite.builder().user(user).club(club1).build();
        favoriteRepository.save(favorite1);
        Thread.sleep(10);
        
        Favorite favorite2 = Favorite.builder().user(user).club(club2).build();
        favoriteRepository.save(favorite2);
        Thread.sleep(10);
        
        Favorite favorite3 = Favorite.builder().user(user).club(club3).build();
        favoriteRepository.save(favorite3);

        FavoriteListServiceRequest request =
                FavoriteListServiceRequest.builder()
                        .userEmail(user.getEmail())
                        .size(2)
                        .sort("latest")
                        .build();

        // when
        List<Favorite> results = favoriteRepository.findFavoritesByRequest(request);

        // then
        assertThat(results).hasSize(3);
        // 최신순 정렬 확인 (unique_club_3 -> 2 -> 1)
        assertThat(results.get(0).getClub().getName()).isEqualTo("unique_club_3");
        assertThat(results.get(1).getClub().getName()).isEqualTo("unique_club_2");
        assertThat(results.get(2).getClub().getName()).isEqualTo("unique_club_1");
    }

    private User createUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPassword("password");
        user.setName("test user");
        return userRepository.save(user);
    }

    private Admin createAdmin(String username, String email) {
        Admin admin = Admin.builder()
                .username(username)
                .password("password123")
                .email(email)
                .build();
        return adminRepository.save(admin);
    }

    private Club createClub(Admin admin, String name) {
        Club club = Club.builder()
                .admin(admin)
                .clubName(name)
                .clubUniv(ClubUniv.ENGINEERING)
                .build();
        return clubRepository.save(club);
    }
}
