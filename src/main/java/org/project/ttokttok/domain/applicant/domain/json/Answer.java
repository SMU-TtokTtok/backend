package org.project.ttokttok.domain.applicant.domain.json;

import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;

import java.util.List;
import java.util.Optional;

public record Answer(
        String title,
        String subTitle,
        QuestionType questionType,
        boolean isEssential,
        List<String> content,
        Object value
) {
    /** 업로드된 파일 답변이면 그 URL. */
    public Optional<String> fileUrl() {
        return questionType == QuestionType.FILE && value instanceof String url && !url.isBlank()
                ? Optional.of(url) : Optional.empty();
    }
}
