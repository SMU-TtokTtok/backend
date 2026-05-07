package org.project.ttokttok.domain.favorite.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteListServiceRequest;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FavoriteRepositoryTest implements RepositoryTestSupport {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private EntityManager em;

    private User testUser;
    private Club testClub1;
    private Club testClub2;

    @BeforeEach
    void setUp() {
        // 관리자 생성
        Admin admin1 = Admin.adminJoin("testadmin1", "password123!", "admin1@sangmyung.kr");
        admin1 = adminRepository.save(admin1);

        Admin admin2 = Admin.adminJoin("testadmin2", "password123!", "admin2@sangmyung.kr");
        admin2 = adminRepository.save(admin2);

        // 동아리 생성
        testClub1 = Club.builder()
                .admin(admin1)
                .clubName("테스트 동아리 1")
                .clubUniv(ClubUniv.ENGINEERING)
                .build();
        testClub1 = clubRepository.save(testClub1);

        testClub2 = Club.builder()
                .admin(admin2)
                .clubName("테스트 동아리 2")
                .clubUniv(ClubUniv.DESIGN)
                .build();
        testClub2 = clubRepository.save(testClub2);

        // 사용자 생성 (Setter 제거 후 정적 팩토리 메서드 사용)
        testUser = User.signUp("test@sangmyung.kr", "password123!", "테스트유저", true);
        testUser = userRepository.save(testUser);

        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        favoriteRepository.deleteAllInBatch();
        clubMemberRepository.deleteAllInBatch();
        clubRepository.deleteAllInBatch();
        adminRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("findByUserEmailAndClubId 메서드")
    class FindByUserEmailAndClubIdTest {

        @Test
        @DisplayName("사용자 이메일과 동아리 ID로 즐겨찾기를 조회한다")
        void findByUserEmailAndClubId_Success() {
            // given
            Favorite favorite = Favorite.builder()
                    .user(testUser)
                    .club(testClub1)
                    .build();
            favoriteRepository.save(favorite);
            em.flush();
            em.clear();

            // when
            Optional<Favorite> result = favoriteRepository.findByUserEmailAndClubId(testUser.getEmail(), testClub1.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getUser().getEmail()).isEqualTo(testUser.getEmail());
            assertThat(result.get().getClub().getId()).isEqualTo(testClub1.getId());
        }
    }

    @Nested
    @DisplayName("countClubFavoritesForEach 메서드")
    class CountClubFavoritesForEachTest {

        @Test
        @DisplayName("주어진 동아리 ID 리스트에 대해 각 동아리별 즐겨찾기 수를 반환한다")
        void countClubFavoritesForEach_ReturnsCountForEachClub() {
            // given
            User user1 = testUser;
            User user2 = User.signUp("user2@sangmyung.kr", "password123!", "유저2", true);
            user2 = userRepository.save(user2);
            User user3 = User.signUp("user3@sangmyung.kr", "password123!", "유저3", true);
            user3 = userRepository.save(user3);

            // testClub1: 즐겨찾기 2개 (user1, user2)
            favoriteRepository.save(Favorite.builder().user(user1).club(testClub1).build());
            favoriteRepository.save(Favorite.builder().user(user2).club(testClub1).build());

            // testClub2: 즐겨찾기 1개 (user3)
            favoriteRepository.save(Favorite.builder().user(user3).club(testClub2).build());

            em.flush();
            em.clear();

            List<String> clubIds = List.of(testClub1.getId(), testClub2.getId());

            // when
            var results = favoriteRepository.countClubFavoritesForEach(clubIds);

            // then
            assertThat(results).hasSize(2);
            
            var club1Result = results.stream()
                    .filter(dto -> dto.clubId().equals(testClub1.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(club1Result.count()).isEqualTo(2);

            var club2Result = results.stream()
                    .filter(dto -> dto.clubId().equals(testClub2.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(club2Result.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("즐겨찾기가 없는 동아리 ID는 결과 리스트에 포함되지 않는다")
        void countClubFavoritesForEach_ExcludesClubsWithNoFavorites() {
            // given
            favoriteRepository.save(Favorite.builder().user(testUser).club(testClub1).build());

            em.flush();
            em.clear();

            List<String> clubIds = List.of(testClub1.getId(), testClub2.getId());

            // when
            var results = favoriteRepository.countClubFavoritesForEach(clubIds);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).clubId()).isEqualTo(testClub1.getId());
            assertThat(results.get(0).count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findFavoritesByRequest (Custom Repository) 메서드")
    class FindFavoritesByRequestTest {

        @Test
        @DisplayName("[최신순] 커서 기반 페이징이 정상 동작한다")
        void findFavoritesByRequest_Latest_CursorPaging() throws InterruptedException {
            // given
            favoriteRepository.save(Favorite.builder().user(testUser).club(testClub1).build());
            em.flush(); 
            
            Thread.sleep(100); // 타임스탬프 차이를 확실히 보장
            
            favoriteRepository.save(Favorite.builder().user(testUser).club(testClub2).build());
            em.flush();
            em.clear();

            // 첫 번째 페이지 요청
            FavoriteListServiceRequest request1 = FavoriteListServiceRequest.builder()
                    .userEmail(testUser.getEmail())
                    .size(1)
                    .sort("latest")
                    .build();

            // when
            List<Favorite> results1 = favoriteRepository.findFavoritesByRequest(request1);

            // then
            assertThat(results1).hasSize(2); 
            assertThat(results1.get(0).getClub().getId()).isEqualTo(testClub2.getId());

            // 두 번째 페이지 요청
            String cursorId = results1.get(0).getId();
            FavoriteListServiceRequest request2 = FavoriteListServiceRequest.builder()
                    .userEmail(testUser.getEmail())
                    .cursor(cursorId)
                    .size(1)
                    .sort("latest")
                    .build();

            List<Favorite> results2 = favoriteRepository.findFavoritesByRequest(request2);

            assertThat(results2).hasSize(1);
            assertThat(results2.get(0).getClub().getId()).isEqualTo(testClub1.getId());
        }

        @Test
        @DisplayName("[멤버수순] 멤버 수가 다를 때 멤버수 기준으로 정렬 및 페이징된다")
        void findFavoritesByRequest_MemberCount_DifferentCount() throws InterruptedException {
            // given
            // testClub2에 멤버 추가 (1명), testClub1은 0명
            addClubMember(testClub2, "멤버1", "member1@test.com");
            
            favoriteRepository.save(Favorite.builder().user(testUser).club(testClub1).build());
            Thread.sleep(10);
            favoriteRepository.save(Favorite.builder().user(testUser).club(testClub2).build());
            
            em.flush();
            em.clear();

            FavoriteListServiceRequest request = FavoriteListServiceRequest.builder()
                    .userEmail(testUser.getEmail())
                    .size(10)
                    .sort("member_count")
                    .build();

            // when
            List<Favorite> results = favoriteRepository.findFavoritesByRequest(request);

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getClub().getId()).isEqualTo(testClub2.getId()); // 멤버 많은 동아리 우선
            assertThat(results.get(1).getClub().getId()).isEqualTo(testClub1.getId());
        }

        @Test
        @DisplayName("[멤버수순] 멤버 수가 같을 때 최신순으로 정렬 및 페이징된다")
        void findFavoritesByRequest_MemberCount_SameCount() throws InterruptedException {
            // given
            // 두 동아리 모두 멤버 0명
            favoriteRepository.save(Favorite.builder().user(testUser).club(testClub1).build());
            em.flush();
            
            Thread.sleep(100);
            
            favoriteRepository.save(Favorite.builder().user(testUser).club(testClub2).build());
            em.flush();
            em.clear();

            // 첫 번째 페이지
            FavoriteListServiceRequest request1 = FavoriteListServiceRequest.builder()
                    .userEmail(testUser.getEmail())
                    .size(1)
                    .sort("member_count")
                    .build();

            // when
            List<Favorite> results1 = favoriteRepository.findFavoritesByRequest(request1);

            // then
            assertThat(results1).hasSize(2);
            assertThat(results1.get(0).getClub().getId()).isEqualTo(testClub2.getId());

            // 두 번째 페이지
            String cursorId = results1.get(0).getId();
            FavoriteListServiceRequest request2 = FavoriteListServiceRequest.builder()
                    .userEmail(testUser.getEmail())
                    .cursor(cursorId)
                    .size(1)
                    .sort("member_count")
                    .build();

            List<Favorite> results2 = favoriteRepository.findFavoritesByRequest(request2);

            assertThat(results2).hasSize(1);
            assertThat(results2.get(0).getClub().getId()).isEqualTo(testClub1.getId());
        }
    }

    private void addClubMember(Club club, String name, String email) {
        ClubMember member = ClubMember.builder()
                .club(club)
                .memberName(name)
                .email(email)
                .phoneNumber("010-0000-0000")
                .role(MemberRole.MEMBER)
                .grade(Grade.FIRST_GRADE)
                .gender(Gender.MALE)
                .major("공학")
                .build();
        clubMemberRepository.save(member);
    }
}
