package org.project.ttokttok.domain.user.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest implements RepositoryTestSupport {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String NONEXISTENT_EMAIL = "nonexistent@example.com";

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setEmail(TEST_EMAIL);
        testUser.setPassword("encodedPassword123");
        testUser.setName("테스트유저");
        userRepository.save(testUser);
    }

    @Nested
    @DisplayName("findByEmail 메서드")
    class FindByEmailTest {

        @Test
        @DisplayName("이메일로 사용자를 조회할 수 있다")
        void findByEmail_ExistingEmail_ReturnsUser() {
            // when
            Optional<User> foundUser = userRepository.findByEmail(TEST_EMAIL);

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
        void findByEmail_NonExistentEmail_ReturnsEmptyOptional() {
            // when
            Optional<User> foundUser = userRepository.findByEmail(NONEXISTENT_EMAIL);

            // then
            assertThat(foundUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail 메서드")
    class ExistsByEmailTest {

        @Test
        @DisplayName("이메일이 존재하면 true를 반환한다")
        void existsByEmail_ExistingEmail_ReturnsTrue() {
            // when
            boolean exists = userRepository.existsByEmail(TEST_EMAIL);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 이메일에 대해 false를 반환한다")
        void existsByEmail_NonExistentEmail_ReturnsFalse() {
            // when
            boolean exists = userRepository.existsByEmail(NONEXISTENT_EMAIL);

            // then
            assertThat(exists).isFalse();
        }
    }
}

