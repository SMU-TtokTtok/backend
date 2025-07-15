package org.project.ttokttok.domain.applicant.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.memo.domain.Memo;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record MemoResponse(
        String id,
        String content
) {
    public static MemoResponse from(Memo memo) {
        return MemoResponse.builder()
                .id(memo.getId())
                .content(memo.getContent())
                .build();
    }

    public static List<MemoResponse> fromList(List<Memo> memos) {
        return memos.stream()
                .map(MemoResponse::from)
                .collect(Collectors.toList());
    }
}
