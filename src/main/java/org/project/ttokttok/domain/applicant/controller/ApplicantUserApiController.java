package org.project.ttokttok.domain.applicant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.controller.docs.ApplicantUserDocs;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplyFormRequest;
import org.project.ttokttok.domain.applicant.service.ApplicantUserService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/applies")
public class ApplicantUserApiController implements ApplicantUserDocs {

    private final ApplicantUserService applicantUserService;

    @PostMapping(
            value = "/{clubId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, String>> apply(@AuthUserInfo String email,
                                                     @PathVariable("clubId") String clubId, // 지원폼 ID
                                                     @Valid @RequestPart ApplyFormRequest request, // json으로 명시 필요
                                                     @RequestPart(required = false) List<String> questionIds, // json으로 명시 필요
                                                     @RequestPart(required = false) List<MultipartFile> files) {

        String ApplicantId = applicantUserService.apply(email, request, questionIds, files, clubId);

        return ResponseEntity.ok()
                .body(Map.of("message", "지원서 작성 완료, id: " + ApplicantId));
    }
}
