package org.project.ttokttok.domain.applyform.controller.dto.response;

import java.util.Map;
import lombok.Builder;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.service.dto.response.ActiveApplyFormServiceResponse;

import java.util.List;

@Builder
public record ActiveApplyFormResponse(
        String formId,
        String title,
        String subTitle,
        List<Question> questions,
        Map<String, Object> tempData
) {
    public static ActiveApplyFormResponse from(ActiveApplyFormServiceResponse response) {
        return ActiveApplyFormResponse.builder()
                .formId(response.formId())
                .title(response.title())
                .subTitle(response.subTitle())
                .questions(response.questions())
                .tempData(response.tempData())
                .build();
    }
}
