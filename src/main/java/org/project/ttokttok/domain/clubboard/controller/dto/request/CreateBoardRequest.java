package org.project.ttokttok.domain.clubboard.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.project.ttokttok.domain.clubboard.service.dto.request.CreateBoardServiceRequest;
import org.springframework.web.multipart.MultipartFile;

public record CreateBoardRequest(
        @NotBlank(message = "제목은 비어 있을 수 없습니다.")
        // 저장 컬럼이 VARCHAR(255) 라 초과하면 DB 단에서 터진다. 여기서 막아 400 으로 돌려준다.
        @Size(max = 255, message = "제목은 최대 255자까지 입력할 수 있습니다.")
        String title,

        @NotBlank(message = "내용은 비어 있을 수 없습니다.")
        String content
) {
    public CreateBoardServiceRequest toServiceRequest(String username, String clubId, MultipartFile thumbnail) {
        return new CreateBoardServiceRequest(username, clubId, title, content, thumbnail);
    }
}
