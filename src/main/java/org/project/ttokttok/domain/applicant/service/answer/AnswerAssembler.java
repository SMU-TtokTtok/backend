package org.project.ttokttok.domain.applicant.service.answer;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.exception.QuestionParseFailException;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.springframework.stereotype.Component;

import static org.project.ttokttok.domain.applyform.domain.enums.QuestionType.FILE;

/** 전체 제출 검증이 끝난 후 파일 업로드와 도메인 답변 생성을 수행한다. */
@Component
@RequiredArgsConstructor
public class AnswerAssembler {
    private final FileAnswerUploader fileAnswerUploader;
    private final AnswerValidator answerValidator;

    public List<Answer> assemble(AnswerSubmission submission, List<Question> questions, String applicantEmail) {
        answerValidator.validate(submission, questions);
        return submission.answers().stream()
                .map(answer -> toAnswer(answer, submission, questions, applicantEmail))
                .toList();
    }

    private Answer toAnswer(AnswerInput input, AnswerSubmission submission,
                            List<Question> questions, String applicantEmail) {
        Question question = questions.stream()
                .filter(candidate -> candidate.questionId().equals(input.questionId()))
                .findFirst().orElseThrow(QuestionParseFailException::new);
        Object value = input.value();
        if (question.questionType() == FILE) {
            value = submission.findFileFor(input.questionId())
                    .map(file -> fileAnswerUploader.upload(file, applicantEmail)).orElse("");
        }
        return new Answer(question.title(), question.subTitle(), question.questionType(),
                question.isEssential(), question.content(), value);
    }
}
