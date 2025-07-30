package org.project.ttokttok.domain.applicant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.controller.docs.ApplicantAdminDocs;
import org.project.ttokttok.domain.applicant.controller.dto.request.SendResultMailRequest;
import org.project.ttokttok.domain.applicant.controller.dto.response.ApplicantDetailResponse;
import org.project.ttokttok.domain.applicant.controller.dto.response.ApplicantFinalizeResponse;
import org.project.ttokttok.domain.applicant.controller.dto.response.ApplicantPageResponse;
import org.project.ttokttok.domain.applicant.controller.enums.Sort;
import org.project.ttokttok.domain.applicant.domain.enums.Status;
import org.project.ttokttok.domain.applicant.service.ApplicantAdminService;
import org.project.ttokttok.domain.applicant.service.dto.request.*;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantDetailServiceResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/applies")
public class ApplicantAdminApiController implements ApplicantAdminDocs {

    private final ApplicantAdminService applicantAdminService;

    @GetMapping
    public ResponseEntity<ApplicantPageResponse> getApplicantsPage(@AuthUserInfo String username,
                                                                   @RequestParam(name = "sort", required = false, defaultValue = "GRADE") Sort sort,
                                                                   @RequestParam(required = false, defaultValue = "false") boolean isEvaluating,
                                                                   @RequestParam(required = false, defaultValue = "1") int cursor,
                                                                   @RequestParam(required = false, defaultValue = "7") int size) {

        ApplicantPageServiceRequest request = ApplicantPageServiceRequest.of(
                username,
                sort.name(),
                isEvaluating,
                cursor,
                size
        );

        ApplicantPageResponse response = ApplicantPageResponse.from(
                applicantAdminService.getApplicantPage(request)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    @GetMapping("/{applicantId}")
    public ResponseEntity<ApplicantDetailResponse> getApplicantDetail(@AuthUserInfo String username,
                                                                      @PathVariable String applicantId) {

        ApplicantDetailServiceResponse applicantDetail = applicantAdminService.getApplicantDetail(username, applicantId);

        ApplicantDetailResponse response = ApplicantDetailResponse.from(applicantDetail);

        return ResponseEntity.ok()
                .body(response);
    }

    // 지원자 검색
    // TODO: 검색도 굳이 페이징이 필요한가? 고민해볼 것.
    @GetMapping("/search")
    public ResponseEntity<ApplicantPageResponse> applicantPageSearch(@AuthUserInfo String username,
                                                                     @RequestParam(name = "name") String keyword,
                                                                     @RequestParam(name = "sort", required = false, defaultValue = "GRADE") Sort sort,
                                                                     @RequestParam(required = false, defaultValue = "false") boolean isEvaluating,
                                                                     @RequestParam(required = false, defaultValue = "1") int cursor,
                                                                     @RequestParam(required = false, defaultValue = "7") int size) {

        ApplicantSearchServiceRequest request = ApplicantSearchServiceRequest.of(
                username,
                keyword,
                sort.name(),
                isEvaluating,
                cursor,
                size
        );

        ApplicantPageResponse response = ApplicantPageResponse.from(
                applicantAdminService.searchApplicantByKeyword(request)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    //FIXME: 하나로 될 거 같음
    // 합격 지원자 목록 조회
    @GetMapping("/passed")
    public ResponseEntity<ApplicantPageResponse> getPassedApplicantsPage(@AuthUserInfo String username,
                                                                         @RequestParam(required = false, defaultValue = "1") int page,
                                                                         @RequestParam(required = false, defaultValue = "4") int size) {

        ApplicantStatusServiceRequest request = ApplicantStatusServiceRequest.of(
                username,
                true,
                page,
                size
        );

        ApplicantPageResponse response = ApplicantPageResponse.from(
                applicantAdminService.getApplicantsByStatus(request)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    // 불합격 지원자 목록 조회
    @GetMapping("/failed")
    public ResponseEntity<ApplicantPageResponse> getFailedApplicantsPage(@AuthUserInfo String username,
                                                                         @RequestParam(required = false, defaultValue = "1") int page,
                                                                         @RequestParam(required = false, defaultValue = "4") int size) {

        ApplicantStatusServiceRequest request = ApplicantStatusServiceRequest.of(
                username,
                false, // passed = false
                page,
                size
        );

        ApplicantPageResponse response = ApplicantPageResponse.from(
                applicantAdminService.getApplicantsByStatus(request)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    // 지원자 상태 업데이트 - status 부분 리팩토링 필요
    @PatchMapping("/evaluations/{applicantId}")
    public ResponseEntity<Void> updateApplicantEvaluation(@AuthUserInfo String username,
                                                          @PathVariable String applicantId,
                                                          @RequestBody Status status) {

        StatusUpdateServiceRequest request = StatusUpdateServiceRequest.of(
                username,
                applicantId,
                status
        );

        applicantAdminService.updateApplicantStatus(request);

        return ResponseEntity.ok()
                .build();
    }

    // 지원자 상태 최종 확정
    @PutMapping("/{clubId}/finalize")
    public ResponseEntity<ApplicantFinalizeResponse> finalizeApplicantsStatus(@AuthUserInfo String username,
                                                                              @PathVariable String clubId) {
        ApplicantFinalizationRequest request = ApplicantFinalizationRequest.of(
                username,
                clubId
        );

        ApplicantFinalizeResponse response = ApplicantFinalizeResponse.from(
                applicantAdminService.finalizeApplicantsStatus(request)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    @PostMapping("/{clubId}/send-email")
    public ResponseEntity<Void> sendEmailToApplicants(@AuthUserInfo String username,
                                                      @PathVariable String clubId,
                                                      @Valid @RequestBody SendResultMailRequest request) {

        applicantAdminService.sendResultMailToApplicants(
                request.toServiceRequest(),
                username,
                clubId
        );

        return ResponseEntity.ok()
                .build();
    }
}
