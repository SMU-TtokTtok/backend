package org.project.ttokttok.domain.memo.service.dto.request;

import lombok.Builder;

@Builder
public record UpdateMemoServiceRequest(
        String memoId,
        String username,
        String applicantId,
        String content
) {
    public static UpdateMemoServiceRequest of(String memoId, String username, String applicantId, String content) {
        return UpdateMemoServiceRequest.builder()
                .memoId(memoId)
                .username(username)
                .applicantId(applicantId)
                .content(content)
                .build();
    }
}
