package org.project.ttokttok.domain.superadmin.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.superadmin.domain.SuperAdmin;
import org.project.ttokttok.domain.superadmin.repository.SuperAdminRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminBootstrapTest {

    private final SuperAdminRepository superAdminRepository = mock(SuperAdminRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("자격증명이 주어지고 계정이 없으면 운영자 계정을 생성한다.")
    void createsWhenAbsent() {
        // given
        when(superAdminRepository.findByUsername("ttok_operator")).thenReturn(Optional.empty());
        SuperAdminBootstrap bootstrap = new SuperAdminBootstrap(
                superAdminRepository, passwordEncoder, "ttok_operator", "rawPassword");

        // when
        bootstrap.run(null);

        // then
        verify(superAdminRepository).save(any(SuperAdmin.class));
    }

    @Test
    @DisplayName("자격증명이 비어 있으면 아무 것도 하지 않는다.")
    void skipsWhenCredentialsBlank() {
        // given
        SuperAdminBootstrap bootstrap = new SuperAdminBootstrap(
                superAdminRepository, passwordEncoder, "", "");

        // when
        bootstrap.run(null);

        // then
        verify(superAdminRepository, never()).save(any(SuperAdmin.class));
        verify(superAdminRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("동일 username 운영자가 이미 존재하면 생성하지 않는다.")
    void skipsWhenAlreadyExists() {
        // given
        when(superAdminRepository.findByUsername("ttok_operator"))
                .thenReturn(Optional.of(mock(SuperAdmin.class)));
        SuperAdminBootstrap bootstrap = new SuperAdminBootstrap(
                superAdminRepository, passwordEncoder, "ttok_operator", "rawPassword");

        // when
        bootstrap.run(null);

        // then
        verify(superAdminRepository, never()).save(any(SuperAdmin.class));
    }
}
