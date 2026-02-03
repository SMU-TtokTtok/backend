package org.project.ttokttok.domain.admin.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.admin.exception.AdminPasswordNotMatchException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    // Test Constants
    private static final String VALID_USERNAME = "testuser123";
    private static final String VALID_PASSWORD = "validPassword123456";
    private static final String ENCODED_PASSWORD = "encodedPassword123";

    // ===== 패스워드 검증 테스트 =====

    @Test
    @DisplayName("올바른 패스워드로 검증하면 예외가 발생하지 않는다")
    void validatePasswordWithCorrectPassword() {
        // given
        Admin admin = Admin.builder()
                .username(VALID_USERNAME)
                .password(ENCODED_PASSWORD)
                .build();

        when(passwordEncoder.matches(VALID_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        // when & then
        assertThatNoException()
                .isThrownBy(() -> admin.validatePassword(VALID_PASSWORD, passwordEncoder));
    }

    @Test
    @DisplayName("잘못된 패스워드로 검증하면 AdminPasswordNotMatchException이 발생한다")
    void validatePasswordWithIncorrectPassword() {
        // given
        final String wrongPassword = "wrongPassword123";
        Admin admin = Admin.builder()
                .username(VALID_USERNAME)
                .password(ENCODED_PASSWORD)
                .build();

        when(passwordEncoder.matches(wrongPassword, ENCODED_PASSWORD)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> admin.validatePassword(wrongPassword, passwordEncoder))
                .isInstanceOf(AdminPasswordNotMatchException.class);
    }

    @Test
    @DisplayName("패스워드 검증 시 PasswordEncoder의 matches 메서드가 호출된다")
    void validatePasswordCallsPasswordEncoderMatches() {
        // given
        Admin admin = Admin.builder()
                .username(VALID_USERNAME)
                .password(ENCODED_PASSWORD)
                .build();

        when(passwordEncoder.matches(VALID_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        // when
        admin.validatePassword(VALID_PASSWORD, passwordEncoder);

        // then
        verify(passwordEncoder).matches(VALID_PASSWORD, ENCODED_PASSWORD);
    }

    // ===== Username 검증 테스트 (@ParameterizedTest 사용) =====

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("null이거나 빈 username으로 생성하면 IllegalArgumentException이 발생한다")
    void createAdminWithInvalidEmptyUsername(String invalidUsername) {
        // when & then
        assertThatThrownBy(() -> Admin.builder()
                .username(invalidUsername)
                .password(VALID_PASSWORD)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리자 명은 null이거나 비어있을 수 없습니다.");
    }

    @ParameterizedTest
    @CsvSource({
            "1234567",
            "abcdefg",
            "test123",
            "user12",
            "short",
            "aaaaaaaaaaaaaaaaaaaaa",
            "verylongusernamethatexceedstwentycharacters",
            "username_that_is_definitely_over_twenty_chars"
    })
    @DisplayName("유효하지 않은 길이의 username으로 생성하면 IllegalArgumentException이 발생한다")
    void createAdminWithInvalidUsernameLength(String invalidUsername) {
        // when & then
        assertThatThrownBy(() -> Admin.builder()
                .username(invalidUsername)
                .password(VALID_PASSWORD)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리자 명은 8자 이상 20자 이하여야 합니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"user@name123", "user name123", "user-name123", "user.name123", "user#123456", "한글유저네임123"})
    @DisplayName("유효하지 않은 문자가 포함된 username으로 생성하면 IllegalArgumentException이 발생한다")
    void createAdminWithInvalidCharactersInUsername(String invalidUsername) {
        // when & then
        assertThatThrownBy(() -> Admin.builder()
                .username(invalidUsername)
                .password(VALID_PASSWORD)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리자 명은 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.");
    }

    // ===== Password 검증 테스트 =====

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("null이거나 빈 password로 생성하면 IllegalArgumentException이 발생한다")
    void createAdminWithInvalidEmptyPassword(String invalidPassword) {
        // when & then
        assertThatThrownBy(() -> Admin.builder()
                .username(VALID_USERNAME)
                .password(invalidPassword)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 null이거나 비어 있을 수 없습니다.");
    }

    // ===== 정상적인 생성 테스트 =====

    @Test
    @DisplayName("adminJoin 정적 팩토리 메서드도 동일한 검증을 수행한다")
    void adminJoinPerformsSameValidation() {
        // when & then
        assertThatThrownBy(() -> Admin.adminJoin(null, VALID_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리자 명은 null이거나 비어있을 수 없습니다.");
    }

    @ParameterizedTest
    @CsvSource({
            "user1234, validPassword123456",
            "aaaaaaaaaaaaaaaaaaaa, validPassword123456",
            "testuser123, password1234",
            "user_123, myLongPassword123456",
            "USERNAME123, UPPERCASEPASS123456"
    })
    @DisplayName("경계값 테스트: 유효한 최소/최대 길이의 username으로 Admin을 생성할 수 있다")
    void createAdminWithBoundaryValidInputs(String username, String password) {
        // when & then
        assertThatNoException()
                .isThrownBy(() -> Admin.builder()
                        .username(username)
                        .password(password)
                        .build());
    }

    @Test
    @DisplayName("경계값 테스트: Username 7자(최소값-1)로 생성하면 예외가 발생한다")
    void createAdminWithUsername7Chars() {
        // given
        final String usernameBelowMinLength = "user123";  // 7자

        // when & then
        assertThatThrownBy(() -> Admin.builder()
                .username(usernameBelowMinLength)
                .password(VALID_PASSWORD)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리자 명은 8자 이상 20자 이하여야 합니다.");
    }

    @Test
    @DisplayName("경계값 테스트: Username 21자(최대값+1)로 생성하면 예외가 발생한다")
    void createAdminWithUsername21Chars() {
        // given
        final String usernameAboveMaxLength = "a".repeat(21); // 21자

        // when & then
        assertThatThrownBy(() -> Admin.builder()
                .username(usernameAboveMaxLength)
                .password(VALID_PASSWORD)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리자 명은 8자 이상 20자 이하여야 합니다.");
    }

    // ===== 비밀번호 업데이트(resetPassword) 테스트 =====

    @Test
    @DisplayName("resetPassword 호출 시 PasswordEncoder.encode가 호출된다")
    void resetPasswordCallsPasswordEncoderEncode() {
        // given
        Admin admin = Admin.builder()
                .username(VALID_USERNAME)
                .password(ENCODED_PASSWORD)
                .build();

        final String newRawPassword = "newPassword456";
        final String newEncodedPassword = "encodedNewPassword456";
        when(passwordEncoder.encode(newRawPassword)).thenReturn(newEncodedPassword);

        // when
        admin.resetPassword(newRawPassword, passwordEncoder);

        // then
        verify(passwordEncoder).encode(newRawPassword);
    }

    @Test
    @DisplayName("resetPassword 후 새 비밀번호로 검증하면 성공한다")
    void resetPasswordSuccessfully() {
        // given
        Admin admin = Admin.builder()
                .username(VALID_USERNAME)
                .password(ENCODED_PASSWORD)
                .build();

        final String newRawPassword = "newPassword456";
        final String newEncodedPassword = "encodedNewPassword456";
        when(passwordEncoder.encode(newRawPassword)).thenReturn(newEncodedPassword);
        when(passwordEncoder.matches(newRawPassword, newEncodedPassword)).thenReturn(true);

        // when
        admin.resetPassword(newRawPassword, passwordEncoder);

        // then
        assertThatNoException()
                .isThrownBy(() -> admin.validatePassword(newRawPassword, passwordEncoder));
    }

    @Test
    @DisplayName("resetPassword 후 기존 비밀번호로 검증하면 실패한다")
    void resetPasswordInvalidatesOldPassword() {
        // given
        Admin admin = Admin.builder()
                .username(VALID_USERNAME)
                .password(ENCODED_PASSWORD)
                .build();

        final String newRawPassword = "newPassword456";
        final String newEncodedPassword = "encodedNewPassword456";
        when(passwordEncoder.encode(newRawPassword)).thenReturn(newEncodedPassword);

        admin.resetPassword(newRawPassword, passwordEncoder);

        // when & then
        when(passwordEncoder.matches(VALID_PASSWORD, newEncodedPassword)).thenReturn(false);
        assertThatThrownBy(() -> admin.validatePassword(VALID_PASSWORD, passwordEncoder))
                .isInstanceOf(AdminPasswordNotMatchException.class);
    }

    @Test
    @DisplayName("resetPassword를 여러 번 호출해도 마지막 비밀번호가 적용된다")
    void resetPasswordMultipleTimes() {
        // given
        Admin admin = Admin.builder()
                .username(VALID_USERNAME)
                .password(ENCODED_PASSWORD)
                .build();

        final String firstNewPassword = "firstPassword123";
        final String secondNewPassword = "secondPassword456";
        final String firstEncoded = "encodedFirst";
        final String secondEncoded = "encodedSecond";

        when(passwordEncoder.encode(firstNewPassword)).thenReturn(firstEncoded);
        when(passwordEncoder.encode(secondNewPassword)).thenReturn(secondEncoded);
        when(passwordEncoder.matches(secondNewPassword, secondEncoded)).thenReturn(true);

        // when
        admin.resetPassword(firstNewPassword, passwordEncoder);
        admin.resetPassword(secondNewPassword, passwordEncoder);

        // then
        assertThatNoException()
                .isThrownBy(() -> admin.validatePassword(secondNewPassword, passwordEncoder));
    }
}
