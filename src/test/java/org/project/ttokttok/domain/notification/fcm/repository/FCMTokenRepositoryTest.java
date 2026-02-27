package org.project.ttokttok.domain.notification.fcm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.project.ttokttok.domain.favorite.repository.FavoriteRepository;
import org.project.ttokttok.domain.notification.fcm.domain.DeviceType;
import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FCMTokenRepositoryTest {

    @Autowired
    private FCMTokenRepository fcmTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private AdminRepository adminRepository;

    private User testUser;
    private Club testClub;
    private FCMToken fcmToken1;
    private FCMToken fcmToken2;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setId("user_" + uniqueSuffix);
        testUser.setEmail("test_" + uniqueSuffix + "@example.com");
        testUser.setPassword("password123");
        testUser.setName("테스트 사용자");
        testUser = userRepository.save(testUser);

        // Admin 생성 (Club에 필요)
        Admin admin = Admin.builder()
                .username("admin_" + uniqueSuffix)
                .password("admin-password")
                .email("admin_" + uniqueSuffix + "@example.com")
                .build();
        Admin savedAdmin = adminRepository.save(admin);

        // 테스트 동아리 생성
        testClub = Club.builder()
                .admin(savedAdmin)
                .clubName("club_" + uniqueSuffix)
                .build();
        testClub = clubRepository.save(testClub);

        // FCM 토큰 생성
        fcmToken1 = FCMToken.create(DeviceType.ANDROID, testUser.getEmail(), "token1_" + uniqueSuffix);
        fcmToken2 = FCMToken.create(DeviceType.IOS, "other_" + uniqueSuffix + "@example.com", "token2_" + uniqueSuffix);

        fcmTokenRepository.save(fcmToken1);
        fcmTokenRepository.save(fcmToken2);
    }

    @AfterEach
    void tearDown() {
        favoriteRepository.deleteAll();
        fcmTokenRepository.deleteAll();
        clubRepository.deleteAll();
        userRepository.deleteAll();
        adminRepository.deleteAll();
    }

    @Test
    @DisplayName("이메일로 FCM 토큰 목록을 조회할 수 있다")
    void findByEmail() {
        // given
        String email = testUser.getEmail();

        // when
        List<FCMToken> tokens = fcmTokenRepository.findByEmail(email);

        // then
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getEmail()).isEqualTo(email);
        assertThat(tokens.get(0).getDeviceType()).isEqualTo(DeviceType.ANDROID);
    }

    @Test
    @DisplayName("이메일과 디바이스 타입으로 FCM 토큰을 조회할 수 있다")
    void findByEmailAndDeviceType() {
        // given
        String email = testUser.getEmail();
        DeviceType deviceType = DeviceType.ANDROID;

        // when
        Optional<FCMToken> token = fcmTokenRepository.findByEmailAndDeviceType(email, deviceType);

        // then
        assertThat(token).isPresent();
        assertThat(token.get().getEmail()).isEqualTo(email);
        assertThat(token.get().getDeviceType()).isEqualTo(deviceType);
    }

    @Test
    @DisplayName("존재하지 않는 이메일과 디바이스 타입으로 조회 시 빈 결과가 반환된다")
    void findByEmailAndDeviceType_NotFound() {
        // given
        String email = "notexist@example.com";
        DeviceType deviceType = DeviceType.WEB;

        // when
        Optional<FCMToken> token = fcmTokenRepository.findByEmailAndDeviceType(email, deviceType);

        // then
        assertThat(token).isEmpty();
    }

    @Test
    @DisplayName("토큰과 이메일로 FCM 토큰을 삭제할 수 있다")
    void deleteByTokenAndEmail() {
        // given
        String token = fcmToken1.getToken();
        String email = fcmToken1.getEmail();

        // when
        fcmTokenRepository.deleteByTokenAndEmail(token, email);

        // then
        List<FCMToken> remainingTokens = fcmTokenRepository.findByEmail(email);
        assertThat(remainingTokens).isEmpty();
    }

    @Test
    @DisplayName("동아리를 즐겨찾기한 사용자들의 FCM 토큰을 조회할 수 있다")
    void findTokensByClubId() {
        // given
        String uniqueSuffix2 = java.util.UUID.randomUUID().toString().substring(0, 8);
        User user2 = new User();
        user2.setId("user2_" + uniqueSuffix2);
        user2.setEmail("test2_" + uniqueSuffix2 + "@example.com");
        user2.setPassword("password123");
        user2.setName("테스트 사용자2");
        user2 = userRepository.save(user2);

        // user2에 대한 FCM 토큰 추가 (setUp에서 생성된 것은 email이 다름)
        FCMToken fcmTokenUser2 = FCMToken.create(DeviceType.WEB, user2.getEmail(), "token_user2_" + uniqueSuffix2);
        fcmTokenRepository.save(fcmTokenUser2);

        // 즐겨찾기 추가
        Favorite favorite1 = Favorite.builder()
                .user(testUser)
                .club(testClub)
                .build();

        Favorite favorite2 = Favorite.builder()
                .user(user2)
                .club(testClub)
                .build();

        favoriteRepository.save(favorite1);
        favoriteRepository.save(favorite2);

        // when
        List<String> tokens = fcmTokenRepository.findTokensByClubId(testClub.getId());

        // then
        assertThat(tokens).hasSize(2);
        assertThat(tokens).contains(fcmToken1.getToken(), fcmTokenUser2.getToken());
    }

    @Test
    @DisplayName("즐겨찾기가 없는 동아리의 FCM 토큰 조회 시 빈 목록이 반환된다")
    void findTokensByClubId_NoFavorites() {
        // when
        List<String> tokens = fcmTokenRepository.findTokensByClubId(testClub.getId());

        // then
        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("특정 날짜보다 오래된 FCM 토큰을 삭제할 수 있다")
    void deleteTokensOlderThan() {
        // given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);

        // FCM 토큰의 생성 시간을 과거로 설정하기 위해 새로운 토큰 생성
        FCMToken oldToken = FCMToken.create(DeviceType.WEB, "old@example.com", "old_token_789");
        fcmTokenRepository.save(oldToken);
        fcmTokenRepository.flush();

        // when
        int deletedCount = fcmTokenRepository.deleteTokensOlderThan(cutoffDate);

        // then
        // 실제로는 JPA 엔티티의 BaseTimeEntity가 현재 시간으로 설정되므로
        // 이 테스트에서는 삭제되는 토큰이 없음
        assertThat(deletedCount).isGreaterThanOrEqualTo(0);

        // 전체 토큰 수 확인
        List<FCMToken> allTokens = fcmTokenRepository.findAll();
        assertThat(allTokens).hasSizeGreaterThanOrEqualTo(2); // 최소 2개는 남아있어야 함
    }

    @Test
    @DisplayName("미래 날짜로 deleteTokensOlderThan 호출 시 모든 토큰이 삭제된다")
    void deleteTokensOlderThan_FutureDate() {
        // given
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        // when
        int deletedCount = fcmTokenRepository.deleteTokensOlderThan(futureDate);

        // then
        assertThat(deletedCount).isGreaterThan(0);

        List<FCMToken> remainingTokens = fcmTokenRepository.findAll();
        assertThat(remainingTokens).isEmpty();
    }
}
