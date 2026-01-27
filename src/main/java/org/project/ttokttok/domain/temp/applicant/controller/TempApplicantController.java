package org.project.ttokttok.domain.temp.applicant.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.temp.applicant.controller.docs.TempApplicantDocs;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempApplicantSaveRequest;
import org.project.ttokttok.domain.temp.applicant.controller.dto.response.TempApplicantDataResponse;
import org.project.ttokttok.domain.temp.applicant.controller.dto.response.TempApplicantSaveResponse;
import org.project.ttokttok.domain.temp.applicant.service.TempApplicantService;
import org.project.ttokttok.domain.temp.applicant.service.dto.request.TempApplicantSaveServiceRequest;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/temp-applicant/{formId}")
public class TempApplicantController implements TempApplicantDocs {

    private final TempApplicantService tempApplicantService;

    /**
     * 임시 지원서를 저장합니다.
     */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<TempApplicantSaveResponse> saveTempApplicant(
            @AuthUserInfo String email,
            @PathVariable String formId,
            @Valid @RequestPart TempApplicantSaveRequest request,
            @RequestPart(required = false) List<String> questionIds,
            @RequestPart(required = false) List<MultipartFile> files) {

        String tempApplicantId = tempApplicantService.saveTempApplicant(
                TempApplicantSaveServiceRequest.of(
                        email,
                        formId,
                        request,
                        questionIds,
                        files
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TempApplicantSaveResponse(tempApplicantId));
    }

    @GetMapping
    public ResponseEntity<TempApplicantDataResponse> getTempApplicant(
            @AuthUserInfo String userEmail,
            @PathVariable String formId) {

        TempApplicantDataResponse response = TempApplicantDataResponse.from(
                tempApplicantService.getTempApplicantData(
                        userEmail,
                        formId
                )
        );

        return ResponseEntity.ok()
                .body(response);
    }
}
