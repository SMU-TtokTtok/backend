package org.project.ttokttok.domain.applicant.controller.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import org.aspectj.weaver.patterns.TypePatternQuestions;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;

import static org.project.ttokttok.domain.applyform.domain.enums.QuestionType.FILE;

public record AnswerRequest(
        @NotBlank(message = "질문 ID는 필수입니다.")
        String questionId,

        /**
         * 답변 내용
         * - 단답형/장문형: 텍스트 값
         * - 체크박스/라디오: 선택된 옵션들
         * - 파일: null (별도의 MultipartFile로 처리)
         */
        @Nullable
        Object value
) {
    public Answer toAnswer(Question question) {
        if (isAnswerInValid(question.isEssential(), question.questionType(), value)) {
            throw new IllegalArgumentException("필수 질문에 대한 답변이 없습니다.");
        }

        return new Answer(
                question.title(),
                question.subTitle(),
                question.questionType(),
                question.isEssential(),
                question.content(),
                value
        );
    }

    private boolean isAnswerInValid(boolean isEssential,
                                    QuestionType questionType,
                                    Object value) {
        // 필수 질문인 경우
        // 1. 파일 타입이 아니고 값이 비어있다면 예외
        // 2. value가 String 문자열이지만 공백으로만 이루어졌거나 null인 경우 예외

        return isEssential &&
                (isNotFileType(questionType, value) ||
                        isBlankString(value));
    }

    private boolean isNotFileType(QuestionType questionType, Object value) {
        return questionType != FILE &&
                value == null;
    }

    private boolean isBlankString(Object value) {
        return value instanceof String &&
                ((String) value).isBlank();
    }
}
