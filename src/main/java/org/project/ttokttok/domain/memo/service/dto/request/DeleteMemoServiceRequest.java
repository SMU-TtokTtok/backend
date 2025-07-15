package org.project.ttokttok.domain.memo.service.dto.request;

import lombok.Builder;

@Builder
public record DeleteMemoServiceRequest(
        String memoId,
        String applicantId,
        String username
) {
    public static DeleteMemoServiceRequest of(String memoId, String applicantId, String username) {
        return DeleteMemoServiceRequest.builder()
                .memoId(memoId)
                .applicantId(applicantId)
                .username(username)
                .build();
    }
}
