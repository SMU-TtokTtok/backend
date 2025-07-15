package org.project.ttokttok.domain.applicant.domain.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;

import java.util.List;

//todo: 차라리 추후에 Question 클래스에 id를 사용하여 값 추가하는 방식이 나을 듯.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Answer(
        String title,
        String subTitle,
        QuestionType questionType,
        boolean isEssential,
        List<String> content,
        Object value
) {
}
