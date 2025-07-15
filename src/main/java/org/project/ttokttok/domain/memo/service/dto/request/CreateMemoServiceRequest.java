package org.project.ttokttok.domain.memo.service.dto.request;

import lombok.Builder;

@Builder
public record CreateMemoServiceRequest(
        String username,
        String applicantId,
        String content
) {
    public static CreateMemoServiceRequest of(String username, String applicantId, String content) {
        return CreateMemoServiceRequest.builder()
                .username(username)
                .applicantId(applicantId)
                .content(content)
                .build();
    }
}
