package org.project.ttokttok.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.exception.AdminNotFoundException;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.admin.service.dto.AdminLoginServiceRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;

    public void login(AdminLoginServiceRequest request) {
        Admin targetAdmin = adminRepository.findByUsername(request.username())
                .orElseThrow(AdminNotFoundException::new);

        targetAdmin.validatePassword(request.password(), passwordEncoder);

        // jwt 반환하기. 헤더에 담아서?
    }
}
