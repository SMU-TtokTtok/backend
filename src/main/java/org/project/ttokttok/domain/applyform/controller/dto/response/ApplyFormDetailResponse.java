package org.project.ttokttok.domain.applyform.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.service.dto.response.ApplyFormDetailServiceResponse;

import java.util.List;

@Builder
public record ApplyFormDetailResponse(
        String title,
        String subTitle,
        List<Question> questions,
        List<BeforeApplyFormResponse> beforeForms
) {
    public static ApplyFormDetailResponse from(ApplyFormDetailServiceResponse response) {
        return ApplyFormDetailResponse.builder()
                .title(response.title())
                .subTitle(response.subTitle())
                .questions(response.questions())
                .beforeForms(response.beforeForms().stream()
                        .map(BeforeApplyFormResponse::from)
                        .toList())
                .build();
    }
}
