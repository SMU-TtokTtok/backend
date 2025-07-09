package org.project.ttokttok.domain.applyform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.controller.dto.request.ApplyFormCreateRequest;
import org.project.ttokttok.domain.applyform.controller.dto.request.ApplyFormUpdateRequest;
import org.project.ttokttok.domain.applyform.controller.dto.response.ApplyFormDetailResponse;
import org.project.ttokttok.domain.applyform.service.ApplyFormAdminService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/forms")
public class ApplyFormAdminApiController {

    private final ApplyFormAdminService applyFormAdminService;

    @PostMapping("/clubs/{clubId}")
    public ResponseEntity<String> createApplyForm(@AuthUserInfo String username,
                                                  @PathVariable String clubId,
                                                  @RequestBody @Valid ApplyFormCreateRequest request) {
        String applyFormId = applyFormAdminService.createApplyForm(
                request.toServiceRequest(username, clubId)
        );

        return ResponseEntity.ok()
                .body("Apply form created successfully with ID: " + applyFormId);
    }

    @GetMapping("/clubs/{clubId}")
    public ResponseEntity<ApplyFormDetailResponse> getApplyFormsByClubId(@AuthUserInfo String username,
                                                                         @PathVariable String clubId) {
        ApplyFormDetailResponse response = ApplyFormDetailResponse.from(
                applyFormAdminService.getApplyFormDetail(username, clubId)
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{formId}")
    public ResponseEntity<String> updateApplyForm(@AuthUserInfo String username,
                                                  @PathVariable String formId,
                                                  @RequestBody ApplyFormUpdateRequest request) {
        applyFormAdminService.updateApplyForm(
                request.toServiceRequest(username, formId)
        );

        return ResponseEntity.noContent()
                .build();
    }
}
