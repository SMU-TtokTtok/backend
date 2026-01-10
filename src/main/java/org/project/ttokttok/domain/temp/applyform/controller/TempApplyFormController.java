package org.project.ttokttok.domain.temp.applyform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.temp.applyform.controller.docs.TempApplyFormDocs;
import org.project.ttokttok.domain.temp.applyform.controller.dto.request.TempApplyFormSaveRequest;
import org.project.ttokttok.domain.temp.applyform.controller.dto.response.TempApplyFormSaveResponse;
import org.project.ttokttok.domain.temp.applyform.service.TempApplyFormService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/temp-applyform")
public class TempApplyFormController implements TempApplyFormDocs {

    private final TempApplyFormService tempApplyFormService;

    /**
     * 임시 지원폼을 저장합니다.
     */
    @PostMapping
    public ResponseEntity<TempApplyFormSaveResponse> saveTempApplyForm(
            @AuthUserInfo String userEmail,
            @Valid @RequestBody TempApplyFormSaveRequest request) {

        String tempApplyFormId = tempApplyFormService.saveTempApplyForm(request);

        return ResponseEntity.ok()
                .body(new TempApplyFormSaveResponse(tempApplyFormId));
    }
}
