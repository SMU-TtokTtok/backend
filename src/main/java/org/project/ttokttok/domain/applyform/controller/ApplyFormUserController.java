package org.project.ttokttok.domain.applyform.controller;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.controller.docs.ApplyFormUserDocs;
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
public class ApplyFormUserController implements ApplyFormUserDocs {

    private final ApplyFormUserService applyFormUserService;

    // TODO: 임시 지원서가 남아있다면 조회
    @GetMapping("/{clubId}")
    public ResponseEntity<ActiveApplyFormResponse> getActiveApplyForm(@PathVariable String clubId) {
        ActiveApplyFormResponse response = ActiveApplyFormResponse.from(
                applyFormUserService.getActiveApplyForm(clubId)
        );

        return ResponseEntity.ok()
                .body(response);
    }
}
