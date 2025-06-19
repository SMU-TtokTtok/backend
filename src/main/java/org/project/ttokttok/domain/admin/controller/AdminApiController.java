package org.project.ttokttok.domain.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.admin.controller.dto.request.AdminLoginRequest;
import org.project.ttokttok.domain.admin.service.AdminService;
import org.project.ttokttok.domain.admin.service.dto.AdminLoginServiceRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminApiController {

    private final AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid AdminLoginRequest request) {
        adminService.login(request.toServiceRequest());

        return null;
    }
}
