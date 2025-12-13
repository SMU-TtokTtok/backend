package org.project.ttokttok.domain.temp.applicant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempApplicantSaveRequest;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.service.TempApplicantService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/temp-applicant")
public class TempApplicantController {

    private final TempApplicantService tempApplicantService;

    /**
     * 임시 지원서를 저장합니다.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> saveTempApplicant(
            @AuthUserInfo String userEmail,
            @Valid @RequestBody TempApplicantSaveRequest request) {

        String tempApplicantId = tempApplicantService.saveTempApplicant(userEmail, request);

        return ResponseEntity.ok()
                .body(Map.of(
                        "message", "임시 지원서가 저장되었습니다.",
                        "tempApplicantId", tempApplicantId
                ));
    }

    /**
     * 특정 지원폼에 대한 임시 지원서를 조회합니다.
     */
    @GetMapping("/{formId}")
    public ResponseEntity<TempApplicant> getTempApplicant(
            @AuthUserInfo String userEmail,
            @PathVariable String formId) {

        Optional<TempApplicant> tempApplicant =
                tempApplicantService.getTempApplicant(userEmail, formId);

        return tempApplicant
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 임시 지원서를 삭제합니다.
     */
    @DeleteMapping("/{formId}")
    public ResponseEntity<Map<String, String>> deleteTempApplicant(
            @AuthUserInfo String userEmail,
            @PathVariable String formId) {

        tempApplicantService.deleteTempApplicant(userEmail, formId);

        return ResponseEntity.ok()
                .body(Map.of("message", "임시 지원서가 삭제되었습니다."));
    }
}
