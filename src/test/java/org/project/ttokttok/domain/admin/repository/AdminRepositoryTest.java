package org.project.ttokttok.domain.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

class AdminRepositoryTest implements RepositoryTestSupport {

    private static final String TEST_USERNAME = "testuser123";
    private static final String NONEXISTENT_USERNAME = "nonexistent";

    @Autowired
    private AdminRepository adminRepository;

    private Admin testAdmin;

    @BeforeEach
    void setUp() {
        final String testPassword = "encodedPassword456";
        testAdmin = Admin.adminJoin(TEST_USERNAME, testPassword);
        adminRepository.save(testAdmin);
    }

    @Test
    @DisplayName("username으로 Admin을 조회할 수 있다")
    void findByUsername_ExistingUsername_ReturnsAdmin() {
        // when
        Optional<Admin> foundAdmin = adminRepository.findByUsername(TEST_USERNAME);

        // then
        assertThat(foundAdmin)
                .isPresent();
        assertThat(foundAdmin.get().getUsername())
                .isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("존재하지 않는 username으로 조회하면 빈 Optional을 반환한다")
    void findByUsername_NonExistentUsername_ReturnsEmptyOptional() {
        // when
        Optional<Admin> foundAdmin = adminRepository.findByUsername(NONEXISTENT_USERNAME);

        // then
        assertThat(foundAdmin).isEmpty();
    }

    @Test
    @DisplayName("username이 존재하는지 확인할 수 있다")
    void existsByUsername_ExistingUsername_ReturnsTrue() {
        // when
        boolean exists = adminRepository.existsByUsername(TEST_USERNAME);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 username에 대해 false를 반환한다")
    void existsByUsername_NonExistentUsername_ReturnsFalse() {
        // when
        boolean exists = adminRepository.existsByUsername(NONEXISTENT_USERNAME);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Admin을 저장할 수 있다")
    void save_ValidAdmin_SavesSuccessfully() {
        // given
        final String newUsername = "newadmin123";
        final String newPassword = "newPassword789";
        Admin newAdmin = Admin.adminJoin(newUsername, newPassword);

        // when
        Admin savedAdmin = adminRepository.save(newAdmin);

        // then
        assertThat(savedAdmin.getId()).isNotNull();
        assertThat(savedAdmin.getUsername()).isEqualTo(newUsername);

        // 데이터베이스에서 실제로 조회 확인
        Optional<Admin> foundAdmin = adminRepository.findByUsername(newUsername);
        assertThat(foundAdmin).isPresent();
    }

    @Test
    @DisplayName("동일한 username을 가진 Admin을 저장하면 예외가 발생한다")
    void save_DuplicateUsername_ThrowsException() {
        // given
        final String differentPassword = "differentPassword";
        Admin duplicateAdmin = Admin.adminJoin(TEST_USERNAME, differentPassword);

        // when & then
        assertThatThrownBy(() -> adminRepository.saveAndFlush(duplicateAdmin))
                .isInstanceOf(Exception.class);
    }
}