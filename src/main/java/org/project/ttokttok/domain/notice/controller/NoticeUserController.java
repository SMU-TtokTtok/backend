package org.project.ttokttok.domain.notice.controller;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.notice.controller.docs.NoticeUserDocs;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeDetailResponse;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeListResponse;
import org.project.ttokttok.domain.notice.service.NoticeUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공지사항 조회 API 컨트롤러
 * 누구나(비로그인 포함) 서비스 공지사항 목록/상세를 조회할 수 있는 API를 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeUserController implements NoticeUserDocs {

    private final NoticeUserService noticeUserService;

    @GetMapping
    public ResponseEntity<NoticeListResponse> getNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(noticeUserService.getNotices(page, size, keyword));
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResponse> getNoticeDetail(@PathVariable String noticeId) {
        return ResponseEntity.ok(noticeUserService.getNoticeDetail(noticeId));
    }
}
