package org.project.ttokttok.domain.temp.applicant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.temp.applicant.controller.docs.TempApplicantDocs;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempApplicantSaveRequest;
import org.project.ttokttok.domain.temp.applicant.controller.dto.response.TempApplicantSaveResponse;
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
public class TempApplicantController implements TempApplicantDocs {

    private final TempApplicantService tempApplicantService;

    /**
     * 임시 지원서를 저장합니다.
     */
    @PostMapping
    public ResponseEntity<TempApplicantSaveResponse> saveTempApplicant(
            @AuthUserInfo String userEmail,
            @Valid @RequestBody TempApplicantSaveRequest request) {

        String tempApplicantId = tempApplicantService.saveTempApplicant(userEmail, request);

        return ResponseEntity.ok()
                .body(new TempApplicantSaveResponse(tempApplicantId));
    }
}
