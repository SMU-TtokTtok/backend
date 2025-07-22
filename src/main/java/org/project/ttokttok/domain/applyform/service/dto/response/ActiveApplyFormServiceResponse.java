package org.project.ttokttok.domain.applyform.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applyform.domain.json.Question;

import java.util.List;

@Builder
public record ActiveApplyFormServiceResponse(
        String title,
        String subTitle,
        List<Question> questions
) {
    public static ActiveApplyFormServiceResponse of(String title,
                                                    String subTitle,
                                                    List<Question> questions
    ) {
        return ActiveApplyFormServiceResponse.builder()
                .title(title)
                .subTitle(subTitle)
                .questions(questions)
                .build();
    }
}
