package org.project.ttokttok.domain.temp.applyform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.temp.applyform.controller.dto.request.TempApplyFormSaveRequest;
import org.project.ttokttok.domain.temp.applyform.domain.TempApplyForm;
import org.project.ttokttok.domain.temp.applyform.service.TempApplyFormService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/temp-applyform")
public class TempApplyFormController {

    private final TempApplyFormService tempApplyFormService;

    /**
     * 임시 지원폼을 저장합니다.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> saveTempApplyForm(
            @AuthUserInfo String userEmail,
            @Valid @RequestBody TempApplyFormSaveRequest request) {

        String tempApplyFormId = tempApplyFormService.saveTempApplyForm(request);

        return ResponseEntity.ok()
                .body(Map.of(
                        "message", "임시 지원폼이 저장되었습니다.",
                        "tempApplyFormId", tempApplyFormId
                ));
    }

    /**
     * 특정 동아리의 임시 지원폼을 조회합니다.
     */
    @GetMapping("/{clubId}")
    public ResponseEntity<TempApplyForm> getTempApplyForm(
            @AuthUserInfo String userEmail,
            @PathVariable String clubId) {

        Optional<TempApplyForm> tempApplyForm =
                tempApplyFormService.getTempApplyForm(clubId);

        return tempApplyForm
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 모든 임시 지원폼을 조회합니다. (관리자용)
     */
    @GetMapping
    public ResponseEntity<List<TempApplyForm>> getAllTempApplyForms(
            @AuthUserInfo String userEmail) {

        List<TempApplyForm> tempApplyForms =
                tempApplyFormService.getAllTempApplyForms();

        return ResponseEntity.ok(tempApplyForms);
    }

    /**
     * 임시 지원폼을 삭제합니다.
     */
    @DeleteMapping("/{clubId}")
    public ResponseEntity<Map<String, String>> deleteTempApplyForm(
            @AuthUserInfo String userEmail,
            @PathVariable String clubId) {

        tempApplyFormService.deleteTempApplyForm(clubId);

        return ResponseEntity.ok()
                .body(Map.of("message", "임시 지원폼이 삭제되었습니다."));
    }

    /**
     * 임시 지원폼 존재 여부를 확인합니다.
     */
    @GetMapping("/exists/{clubId}")
    public ResponseEntity<Map<String, Boolean>> existsTempApplyForm(
            @AuthUserInfo String userEmail,
            @PathVariable String clubId) {

        boolean exists = tempApplyFormService.existsTempApplyForm(clubId);

        return ResponseEntity.ok()
                .body(Map.of("exists", exists));
    }
}
