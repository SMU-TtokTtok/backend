package org.project.ttokttok.domain.applicant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.controller.docs.ApplicantUserDocs;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplyFormRequest;
import org.project.ttokttok.domain.applicant.service.ApplicantUserService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applies")
public class ApplicantUserApiController implements ApplicantUserDocs {

    private final ApplicantUserService applicantUserService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> apply(@AuthUserInfo String email,
                                      @Valid @RequestPart ApplyFormRequest request,
                                      @RequestPart(required = false) Map<String, MultipartFile> files) {

        return ResponseEntity.ok()
                .build();
    }
}
