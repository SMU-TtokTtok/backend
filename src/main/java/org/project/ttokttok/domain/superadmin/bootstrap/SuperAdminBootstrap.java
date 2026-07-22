package org.project.ttokttok.domain.superadmin.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.superadmin.domain.SuperAdmin;
import org.project.ttokttok.domain.superadmin.repository.SuperAdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자(ROLE_SUPER_ADMIN) 계정 부트스트랩.
 * 자격증명을 저장소에 커밋하지 않기 위해, 앱 시작 시 환경변수(시크릿)로 주입된
 * SUPER_ADMIN_USERNAME / SUPER_ADMIN_PASSWORD 로 운영자 계정을 멱등적으로 생성한다.
 * - 자격증명이 비어 있으면 아무 것도 하지 않는다(로컬/테스트 등).
 * - 동일 username 계정이 이미 있으면 생성하지 않는다.
 */
@Slf4j
@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public SuperAdminBootstrap(
            SuperAdminRepository superAdminRepository,
            PasswordEncoder passwordEncoder,
            @Value("${super.admin.username:}") String bootstrapUsername,
            @Value("${super.admin.password:}") String bootstrapPassword
    ) {
        this.superAdminRepository = superAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapUsername.isBlank() || bootstrapPassword.isBlank()) {
            log.info("운영자 부트스트랩 자격증명 미설정 - 건너뜁니다. (SUPER_ADMIN_USERNAME/PASSWORD)");
            return;
        }

        if (superAdminRepository.findByUsername(bootstrapUsername).isPresent()) {
            return; // 이미 존재 -> 멱등적으로 아무 것도 하지 않음
        }

        SuperAdmin superAdmin = SuperAdmin.create(
                bootstrapUsername,
                passwordEncoder.encode(bootstrapPassword)
        );
        superAdminRepository.save(superAdmin);

        log.info("운영자 계정 부트스트랩 완료: username={}", bootstrapUsername);
    }
}
