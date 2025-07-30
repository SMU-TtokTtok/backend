package org.project.ttokttok.domain.applyform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.controller.docs.ApplyFormAdminDocs;
import org.project.ttokttok.domain.applyform.controller.dto.request.ApplyFormCreateRequest;
import org.project.ttokttok.domain.applyform.controller.dto.request.ApplyFormUpdateRequest;
import org.project.ttokttok.domain.applyform.controller.dto.response.ApplyFormCreateResponse;
import org.project.ttokttok.domain.applyform.controller.dto.response.ApplyFormDetailResponse;
import org.project.ttokttok.domain.applyform.controller.dto.response.BeforeQuestionsResponse;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.service.ApplyFormAdminService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/forms")
public class ApplyFormAdminApiController implements ApplyFormAdminDocs {

    private final ApplyFormAdminService applyFormAdminService;

    // 폼 생성 로직
    @PostMapping("/clubs/{clubId}")
    public ResponseEntity<ApplyFormCreateResponse> createApplyForm(@AuthUserInfo String username,
                                                                   @PathVariable String clubId,
                                                                   @RequestBody @Valid ApplyFormCreateRequest request) {
        String applyFormId = applyFormAdminService.createApplyForm(
                request.toServiceRequest(username, clubId)
        );

        ApplyFormCreateResponse response = new ApplyFormCreateResponse(applyFormId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // 지원 폼 상세 조회(현재 활성화된 폼)
    @GetMapping("/{clubId}")
    public ResponseEntity<ApplyFormDetailResponse> getApplyFormsByClubId(@AuthUserInfo String username,
                                                                         @PathVariable String clubId) {
        ApplyFormDetailResponse response = ApplyFormDetailResponse.from(
                applyFormAdminService.getApplyFormDetail(username, clubId)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    // 지원 폼 수정하기
    @PatchMapping("/{formId}")
    public ResponseEntity<Void> updateApplyForm(@AuthUserInfo String username,
                                                @PathVariable String formId,
                                                @RequestBody ApplyFormUpdateRequest request) {
        applyFormAdminService.updateApplyForm(
                request.toServiceRequest(username, formId)
        );

        return ResponseEntity.ok()
                .build();
    }

    // 이전에 만들어둔 지원 폼 질문 형태 받아오기
    @GetMapping("/before/{formId}")
    public ResponseEntity<BeforeQuestionsResponse> getPreviousQuestions(@AuthUserInfo String username,
                                                                        @PathVariable String formId) {

        List<Question> questions = applyFormAdminService.getPreviousApplyFormQuestions(username, formId);
        BeforeQuestionsResponse response = new BeforeQuestionsResponse(questions);

        return ResponseEntity.ok()
                .body(response);
    }
}
