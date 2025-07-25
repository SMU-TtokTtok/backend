package org.project.ttokttok.domain.applyform.controller;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.controller.dto.request.ApplyFormSubmitRequest;
import org.project.ttokttok.domain.applyform.controller.dto.response.ActiveApplyFormResponse;
import org.project.ttokttok.domain.applyform.service.ApplyFormUserService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/forms") // 임시, 추후 변경 가능성 있음.
public class ApplyFormUserController {

    // TODO: 사용자 지원 폼 조회 API, 사용자 지원 폼 제출 API 구현
    /**
     * 구상하고 있는 로직
     * 조회
     * 1. 접근한 clubId에 해당하고 활성화된 지원 폼을 조회
     * 2. 해당 폼에 대한 질문들(JSONB 형태로 저장된 질문들)을 조회
     * <p>
     * 제출
     * 1. 별도의 POST API를 통해 지원 폼 제출
     * 유의사항: 응답 양식의 복잡도가 있는 편이라, 양식에 유의할 것.
     */

    private final ApplyFormUserService applyFormUserService;

    @GetMapping("/{clubId}")
    public ResponseEntity<ActiveApplyFormResponse> getActiveApplyForm(@PathVariable String clubId) {
        ActiveApplyFormResponse response = ActiveApplyFormResponse.from(
                applyFormUserService.getActiveApplyForm(clubId)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    @PostMapping(value = "/{formId}/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> submitApplyForm(@AuthUserInfo String email,
                                                @PathVariable String formId,
                                                @RequestPart ApplyFormSubmitRequest request) {

        applyFormUserService.submitApplyForm();

        return ResponseEntity.noContent()
                .build();
    }
}
