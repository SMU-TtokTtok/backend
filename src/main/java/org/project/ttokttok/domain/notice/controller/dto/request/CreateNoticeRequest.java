package org.project.ttokttok.domain.notice.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.project.ttokttok.domain.notice.service.dto.request.CreateNoticeServiceRequest;

public record CreateNoticeRequest(
        @NotBlank(message = "제목은 비어 있을 수 없습니다.")
        String title,

        @NotBlank(message = "내용은 비어 있을 수 없습니다.")
        String content
) {
    public CreateNoticeServiceRequest toServiceRequest(String username) {
        return new CreateNoticeServiceRequest(username, title, content);
    }
}
