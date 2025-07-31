package org.project.ttokttok.domain.memo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.memo.controller.docs.MemoDocs;
import org.project.ttokttok.domain.memo.controller.dto.request.MemoRequest;
import org.project.ttokttok.domain.memo.controller.dto.response.MemoCreateResponse;
import org.project.ttokttok.domain.memo.service.MemoService;
import org.project.ttokttok.domain.memo.service.dto.request.CreateMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.DeleteMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.UpdateMemoServiceRequest;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/applies/{applicantId}/memos")
public class MemoApiController implements MemoDocs {

    private final MemoService memoService;

    // 메모 생성
    @PostMapping
    public ResponseEntity<MemoCreateResponse> createMemo(@AuthUserInfo String username,
                                                         @PathVariable String applicantId,
                                                         @Valid @RequestBody MemoRequest request) {

        var serviceRequest = CreateMemoServiceRequest.of(
                username,
                applicantId,
                request.content()
        );

        String memoId = memoService.createMemo(serviceRequest);

        MemoCreateResponse response = new MemoCreateResponse(memoId);

        return ResponseEntity.ok()
                .body(response);
    }

    // 메모 수정
    @PatchMapping("/{memoId}")
    public ResponseEntity<Map<String, String>> updateMemo(@AuthUserInfo String username,
                                                          @PathVariable String applicantId,
                                                          @PathVariable String memoId,
                                                          @Valid @RequestBody MemoRequest request) {

        var serviceRequest = UpdateMemoServiceRequest.of(
                memoId,
                username,
                applicantId,
                request.content()
        );

        memoService.updateMemo(serviceRequest);

        return ResponseEntity.ok()
                .body(Map.of("message", "메모 수정에 성공했습니다."));
    }

    // 메모 삭제
    @DeleteMapping("/{memoId}")
    public ResponseEntity<Map<String, String>> deleteMemo(@AuthUserInfo String username,
                                                          @PathVariable String applicantId,
                                                          @PathVariable String memoId) {

        var serviceRequest = DeleteMemoServiceRequest.of(
                memoId,
                applicantId,
                username
        );

        memoService.deleteMemo(serviceRequest);

        return ResponseEntity.ok()
                .body(Map.of("message", "메모 삭제에 성공했습니다."));
    }
}
