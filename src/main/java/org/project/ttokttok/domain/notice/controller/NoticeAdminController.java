package org.project.ttokttok.domain.notice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.notice.controller.docs.NoticeAdminDocs;
import org.project.ttokttok.domain.notice.controller.dto.request.CreateNoticeRequest;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeCreateResponse;
import org.project.ttokttok.domain.notice.service.NoticeAdminService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공지사항 관리 API 컨트롤러
 * 서비스 운영/유지보수 팀(ROLE_SUPER_ADMIN)이 전역 공지사항을 저장하는 API를 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/super-admin/notices")
public class NoticeAdminController implements NoticeAdminDocs {

    private final NoticeAdminService noticeAdminService;

    @PostMapping
    public ResponseEntity<NoticeCreateResponse> createNotice(
            @AuthUserInfo String username,
            @RequestBody @Valid CreateNoticeRequest request
    ) {
        String noticeId = noticeAdminService.createNotice(request.toServiceRequest(username));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new NoticeCreateResponse(noticeId));
    }
}
