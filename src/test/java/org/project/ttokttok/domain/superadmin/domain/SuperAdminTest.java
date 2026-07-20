package org.project.ttokttok.domain.superadmin.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.superadmin.exception.SuperAdminPasswordNotMatchException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperAdminTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("운영자 계정을 생성하면 아이디가 설정된다.")
    void createSuccess() {
        // when
        SuperAdmin superAdmin = SuperAdmin.create("ttok_operator", passwordEncoder.encode("rawPassword"));

        // then
        assertThat(superAdmin.getUsername()).isEqualTo("ttok_operator");
    }

    @Test
    @DisplayName("올바른 비밀번호로 검증하면 예외가 발생하지 않는다.")
    void validatePasswordSuccess() {
        // given
        SuperAdmin superAdmin = SuperAdmin.create("ttok_operator", passwordEncoder.encode("rawPassword"));

        // when & then
        assertThatCode(() -> superAdmin.validatePassword("rawPassword", passwordEncoder))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("잘못된 비밀번호로 검증하면 예외가 발생한다.")
    void validatePasswordMismatch() {
        // given
        SuperAdmin superAdmin = SuperAdmin.create("ttok_operator", passwordEncoder.encode("rawPassword"));

        // when & then
        assertThatThrownBy(() -> superAdmin.validatePassword("wrongPassword", passwordEncoder))
                .isInstanceOf(SuperAdminPasswordNotMatchException.class);
    }
}
