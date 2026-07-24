package org.project.ttokttok.domain.notice.controller.dto.response;

import org.project.ttokttok.domain.notice.domain.Notice;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeListResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<NoticeSummary> notices
) {

    // 목록 한 행에 노출되는 요약 정보 (제목 / 작성일 / 조회수)
    public record NoticeSummary(
            String noticeId,
            String title,
            LocalDateTime createdAt,
            int viewCount
    ) {
        public static NoticeSummary from(Notice notice) {
            return new NoticeSummary(
                    notice.getId(),
                    notice.getTitle(),
                    notice.getCreatedAt(),
                    notice.getViewCount()
            );
        }
    }

    public static NoticeListResponse of(int currentPage, int totalPage, int totalCount, List<NoticeSummary> notices) {
        return new NoticeListResponse(currentPage, totalPage, totalCount, notices);
    }
}
