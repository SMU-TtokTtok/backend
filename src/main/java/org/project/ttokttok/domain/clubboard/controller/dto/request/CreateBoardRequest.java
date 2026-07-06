package org.project.ttokttok.domain.clubboard.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.project.ttokttok.domain.clubboard.service.dto.request.CreateBoardServiceRequest;

public record CreateBoardRequest(
        @NotBlank(message = "제목은 비어 있을 수 없습니다.")
        String title,

        @NotBlank(message = "내용은 비어 있을 수 없습니다.")
        String content
) {
    public CreateBoardServiceRequest toServiceRequest(String username, String clubId) {
        return new CreateBoardServiceRequest(username, clubId, title, content);
    }
}
