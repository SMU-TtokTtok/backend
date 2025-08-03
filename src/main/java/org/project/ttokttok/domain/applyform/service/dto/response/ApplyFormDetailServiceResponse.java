package org.project.ttokttok.domain.applyform.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applyform.domain.json.Question;

import java.util.List;

@Builder
public record ApplyFormDetailServiceResponse(
        String formId,
        String title,
        String subTitle,
        List<Question> questions,
        List<BeforeApplyFormServiceResponse> beforeForms
) {
    public static ApplyFormDetailServiceResponse of(String formId,
                                                    String title,
                                                    String subTitle,
                                                    List<Question> questions,
                                                    List<BeforeApplyFormServiceResponse> beforeForms) {
        return ApplyFormDetailServiceResponse.builder()
                .formId(formId)
                .title(title)
                .subTitle(subTitle)
                .questions(questions)
                .beforeForms(beforeForms)
                .build();
    }
}
