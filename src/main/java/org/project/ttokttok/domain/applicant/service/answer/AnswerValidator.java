package org.project.ttokttok.domain.applicant.service.answer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.project.ttokttok.domain.applicant.exception.AnswerRequestNotMatchException;
import org.project.ttokttok.domain.applicant.exception.InvalidAnswerException;
import org.project.ttokttok.domain.applicant.exception.ListSizeNotMatchException;
import org.project.ttokttok.domain.applicant.exception.QuestionParseFailException;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.springframework.stereotype.Component;

import static org.project.ttokttok.domain.applyform.domain.enums.QuestionType.FILE;

/** 최종 제출 전체를 검증한다. 파일 업로드나 영속 상태 변경은 수행하지 않는다. */
@Component
public class AnswerValidator {
    public void validate(AnswerSubmission submission, List<Question> questions) {
        validateFilePairs(submission);
        Set<String> answeredIds = validateAnswers(submission, questions);
        validateFileIds(submission, questions, answeredIds);
        validateRequiredQuestions(submission, questions, answeredIds);
    }

    private void validateFilePairs(AnswerSubmission submission) {
        if (submission.hasNoFileInput()) {
            return;
        }
        if (!submission.hasQuestionIds() || submission.fileCount() == 0) {
            throw new AnswerRequestNotMatchException();
        }
        if (submission.questionIds().size() != submission.fileCount()) {
            throw new ListSizeNotMatchException();
        }
    }

    private Set<String> validateAnswers(AnswerSubmission submission, List<Question> questions) {
        Set<String> ids = new HashSet<>();
        for (AnswerInput answer : submission.answers()) {
            if (answer == null || answer.questionId() == null || answer.questionId().isBlank()
                    || !ids.add(answer.questionId())) {
                throw new InvalidAnswerException();
            }
            validateValue(findQuestion(questions, answer.questionId()), answer.value());
        }
        return ids;
    }

    private void validateFileIds(AnswerSubmission submission, List<Question> questions, Set<String> answeredIds) {
        if (!submission.hasQuestionIds()) {
            return;
        }
        Set<String> fileIds = new HashSet<>();
        for (String id : submission.questionIds()) {
            if (id == null || id.isBlank() || !fileIds.add(id) || !answeredIds.contains(id)
                    || findQuestion(questions, id).questionType() != FILE) {
                throw new InvalidAnswerException();
            }
        }
    }

    private void validateRequiredQuestions(AnswerSubmission submission, List<Question> questions,
                                           Set<String> answeredIds) {
        for (Question question : questions) {
            if (!question.isEssential()) {
                continue;
            }
            if (question.questionType() == FILE) {
                if (!answeredIds.contains(question.questionId())
                        || submission.findFileFor(question.questionId()).isEmpty()) {
                    throw new AnswerRequestNotMatchException();
                }
            } else if (!answeredIds.contains(question.questionId())) {
                throw new InvalidAnswerException();
            }
        }
    }

    private void validateValue(Question question, Object value) {
        if (question.questionType() == FILE) {
            if (value != null) {
                throw new InvalidAnswerException();
            }
            return;
        }
        if (question.isEssential() && (value == null || value instanceof String text && text.isBlank())) {
            throw new IllegalArgumentException("필수 질문에 대한 답변이 없습니다.");
        }
        if (value != null) {
            validateTypeAndChoices(question, value);
        }
    }

    private void validateTypeAndChoices(Question question, Object value) {
        switch (question.questionType()) {
            case SHORT_ANSWER, LONG_ANSWER -> {
                if (!(value instanceof String)) {
                    throw new InvalidAnswerException();
                }
            }
            case RADIO -> {
                if (!(value instanceof String choice) || !isAllowedChoice(question, choice)) {
                    throw new InvalidAnswerException();
                }
            }
            case CHECKBOX -> validateCheckbox(question, value);
            default -> throw new InvalidAnswerException();
        }
    }

    private void validateCheckbox(Question question, Object value) {
        if (!(value instanceof List<?> choices) || question.isEssential() && choices.isEmpty()) {
            throw new InvalidAnswerException();
        }
        Set<String> selected = new HashSet<>();
        for (Object item : choices) {
            if (!(item instanceof String choice) || !isAllowedChoice(question, choice) || !selected.add(choice)) {
                throw new InvalidAnswerException();
            }
        }
    }

    private boolean isAllowedChoice(Question question, String choice) {
        return question.content() != null && question.content().contains(choice);
    }

    private Question findQuestion(List<Question> questions, String id) {
        return questions.stream().filter(question -> question.questionId().equals(id))
                .findFirst().orElseThrow(QuestionParseFailException::new);
    }
}
