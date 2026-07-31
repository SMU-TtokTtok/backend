package org.project.ttokttok.domain.applicant.service.answer;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.controller.dto.request.AnswerRequest;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.exception.AnswerRequestNotMatchException;
import org.project.ttokttok.domain.applicant.exception.ListSizeNotMatchException;
import org.project.ttokttok.domain.applicant.exception.QuestionParseFailException;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.project.ttokttok.domain.applyform.domain.enums.QuestionType.FILE;

/**
 * 지원서 제출 데이터를 도메인 답변({@link Answer})으로 조립한다.
 *
 * <p>파일 질문과 일반 질문의 처리 차이, 그리고 그에 따른 유효성 검사를 이 클래스가 전담합니다.
 * 파일 업로드 자체는 {@link FileAnswerUploader} 에 위임하므로 이 클래스는 저장소 구현에
 * 의존하지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class AnswerAssembler {

    private final FileAnswerUploader fileAnswerUploader;

    /**
     * 제출된 답변을 검증한 뒤 {@link Answer} 목록으로 변환한다.
     *
     * @param submission     제출된 답변 원본 데이터
     * @param questions      지원폼에 정의된 질문 목록
     * @param applicantEmail 지원자 이메일 (파일 업로드 경로에 사용)
     * @return 조립된 답변 목록
     * @throws AnswerRequestNotMatchException 필수 파일 질문에 파일이 없는 경우
     * @throws ListSizeNotMatchException      파일 질문 개수와 파일 개수가 일치하지 않는 경우
     * @throws QuestionParseFailException     답변의 질문 ID가 지원폼에 없는 경우
     */
    public List<Answer> assemble(AnswerSubmission submission, List<Question> questions, String applicantEmail) {
        validateFileConsistency(submission, questions);

        return submission.answers().stream()
                .map(answerRequest -> toAnswer(answerRequest, submission, questions, applicantEmail))
                .toList();
    }

    private Answer toAnswer(AnswerRequest answerRequest,
                            AnswerSubmission submission,
                            List<Question> questions,
                            String applicantEmail) {

        Question question = findQuestion(questions, answerRequest.questionId());

        if (question.questionType() != FILE) {
            return answerRequest.toAnswer(question);
        }

        // 파일 질문이 필수가 아니면 파일 없이 제출될 수 있으므로, 이때는 빈 값으로 처리한다.
        String fileUrl = submission.findFileFor(answerRequest.questionId())
                .map(file -> fileAnswerUploader.upload(file, applicantEmail))
                .orElse("");

        return new AnswerRequest(answerRequest.questionId(), fileUrl).toAnswer(question);
    }

    private Question findQuestion(List<Question> questions, String questionId) {
        return questions.stream()
                .filter(question -> question.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(QuestionParseFailException::new);
    }

    /**
     * 파일 질문 ID 목록과 업로드된 파일 목록의 정합성을 검사한다.
     */
    private void validateFileConsistency(AnswerSubmission submission, List<Question> questions) {
        // 1. 파일 관련 입력이 아예 없는 경우 — 필수 파일 질문이 있으면 잘못된 요청이다.
        if (submission.hasNoFileInput()) {
            if (hasRequiredFileQuestion(questions)) {
                throw new AnswerRequestNotMatchException();
            }
            return;
        }

        // 2. 파일 질문 ID가 전달됐다면 파일 개수와 일치해야 한다.
        if (!submission.hasQuestionIds()) {
            return;
        }

        List<String> fileQuestionIds = submission.questionIds().stream()
                .filter(questionId -> isFileQuestion(questions, questionId))
                .toList();

        if (fileQuestionIds.isEmpty()) {
            return;
        }

        if (submission.fileCount() == 0) {
            throw new AnswerRequestNotMatchException();
        }

        if (fileQuestionIds.size() != submission.fileCount()) {
            throw new ListSizeNotMatchException();
        }
    }

    private boolean hasRequiredFileQuestion(List<Question> questions) {
        return questions.stream()
                .anyMatch(question -> question.questionType() == FILE && question.isEssential());
    }

    private boolean isFileQuestion(List<Question> questions, String questionId) {
        return questions.stream()
                .anyMatch(question -> question.questionId().equals(questionId)
                        && question.questionType() == FILE);
    }
}
