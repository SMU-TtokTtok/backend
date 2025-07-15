package org.project.ttokttok.domain.memo.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemoRequest(
        @NotBlank(message = "메모 내용은 필수입니다.")
        @Size(max = 100, message = "메모 내용은 최대 100자까지 입력할 수 있습니다.")
        String content
) {
}
