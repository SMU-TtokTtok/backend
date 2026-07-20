package org.project.ttokttok.domain.notice.service.dto.request;

public record CreateNoticeServiceRequest(
        String username,
        String title,
        String content
) {
}
