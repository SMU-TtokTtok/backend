package org.project.ttokttok.domain.notice.controller.dto.response;

import org.project.ttokttok.domain.notice.domain.Notice;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        String noticeId,
        String title,
        String content,
        String createdBy,
        LocalDateTime createdAt,
        int viewCount
) {
    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedBy(),
                notice.getCreatedAt(),
                notice.getViewCount()
        );
    }
}
