package org.project.ttokttok.domain.notice.repository.dto;

import lombok.Builder;
import org.project.ttokttok.domain.notice.domain.Notice;

import java.util.List;

@Builder
public record NoticePageQueryResponse(
        List<Notice> content,
        long totalCount
) {
}
