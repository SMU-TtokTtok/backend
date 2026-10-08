package org.project.ttokttok.domain.applicant.service.answer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applicant.exception.InvalidAnswerException;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.exception.AnswerRequestNotMatchException;
import org.project.ttokttok.domain.applicant.exception.ListSizeNotMatchException;
import org.project.ttokttok.domain.applicant.exception.QuestionParseFailException;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link AnswerAssembler} 단위 테스트.
 *
 * <p>정상 답변의 값과 순서를 보존하고, 제출 전체 검증이 끝난 후 업로드하는지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerAssembler - 지원서 답변 조립")
class AnswerAssemblerTest {

    private static final String EMAIL = "user@sangmyung.kr";
    private static final String UPLOAD_PATH = "applicant/" + EMAIL + "/";

    @Mock
    private org.project.ttokttok.infrastructure.s3.service.S3Service s3Service;

    private AnswerAssembler answerAssembler;

    @BeforeEach
    void setUp() {
        answerAssembler = new AnswerAssembler(new FileAnswerUploader(s3Service), new AnswerValidator());
    }

    private Question fileQuestion(String id, boolean essential) {
        return new Question(id, "파일질문-" + id, null, QuestionType.FILE, essential, List.of());
    }

    private Question textQuestion(String id) {
        return new Question(id, "단답질문-" + id, null, QuestionType.SHORT_ANSWER, false, List.of());
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile(name, name + ".pdf", "application/pdf", ("내용-" + name).getBytes());
    }

    @Test
    @DisplayName("뒤쪽 필수 답변이 잘못되어도 앞쪽 파일을 업로드하지 않는다")
    void validatesAllAnswersBeforeUpload() {
        List<Question> questions = List.of(fileQuestion("f1", true),
                new Question("q1", "필수", null, QuestionType.SHORT_ANSWER, true, List.of()));
        AnswerSubmission submission = new AnswerSubmission(
                List.of(new AnswerInput("f1", null), new AnswerInput("q1", " ")),
                List.of("f1"), List.of(file("f1")));

        assertThatThrownBy(() -> answerAssembler.assemble(submission, questions, EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 질문에 대한 답변이 없습니다.");
        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("필수 일반 질문의 답변이 누락되면 거부한다")
    void rejectsMissingRequiredAnswer() {
        List<Question> questions = List.of(
                new Question("q1", "필수", null, QuestionType.SHORT_ANSWER, true, List.of()));

        assertThatThrownBy(() -> answerAssembler.assemble(
                new AnswerSubmission(List.of(), null, null), questions, EMAIL))
                .isInstanceOf(InvalidAnswerException.class);
        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Nested
    @DisplayName("파일이 아닌 질문")
    class NonFileQuestions {

        @Test
        @DisplayName("답변 값을 그대로 담고 S3를 호출하지 않는다")
        void passesThroughWithoutUpload() {
            List<Question> questions = List.of(textQuestion("q1"), textQuestion("q2"));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("q1", "답변1"), new AnswerInput("q2", "답변2")),
                    null, null);

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).extracting(Answer::value).containsExactly("답변1", "답변2");
            assertThat(answers).extracting(Answer::questionType)
                    .containsOnly(QuestionType.SHORT_ANSWER);
            verify(s3Service, never()).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("답변 순서는 요청 순서를 그대로 유지한다")
        void preservesAnswerOrder() {
            List<Question> questions = List.of(textQuestion("q1"), textQuestion("q2"), textQuestion("q3"));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("q3", "셋"),
                            new AnswerInput("q1", "하나"),
                            new AnswerInput("q2", "둘")),
                    null, null);

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).extracting(Answer::value).containsExactly("셋", "하나", "둘");
        }
    }

    @Nested
    @DisplayName("파일 질문 - 업로드 성공 경로")
    class FileUploadSuccess {

        @Test
        @DisplayName("questionIds와 files의 같은 인덱스끼리 짝지어 업로드하고 URL을 답변으로 담는다")
        void uploadsAndStoresUrl() {
            List<Question> questions = List.of(fileQuestion("f1", true));
            MultipartFile uploaded = file("f1");
            given(s3Service.uploadFile(uploaded, UPLOAD_PATH)).willReturn("https://s3/f1.pdf");

            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null)),
                    List.of("f1"), List.of(uploaded));

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).singleElement()
                    .extracting(Answer::value).isEqualTo("https://s3/f1.pdf");
            verify(s3Service).uploadFile(uploaded, UPLOAD_PATH);
        }

        @Test
        @DisplayName("파일 질문이 여러 개면 questionIds 순서에 맞는 파일이 각각 짝지어진다")
        void pairsMultipleFilesByIndex() {
            List<Question> questions = List.of(fileQuestion("f1", true), fileQuestion("f2", true));
            MultipartFile first = file("first");
            MultipartFile second = file("second");
            given(s3Service.uploadFile(first, UPLOAD_PATH)).willReturn("https://s3/first.pdf");
            given(s3Service.uploadFile(second, UPLOAD_PATH)).willReturn("https://s3/second.pdf");

            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null), new AnswerInput("f2", null)),
                    List.of("f1", "f2"), List.of(first, second));

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).extracting(Answer::value)
                    .containsExactly("https://s3/first.pdf", "https://s3/second.pdf");
        }

        @Test
        @DisplayName("questionIds 순서가 답변 순서와 달라도 questionId 기준으로 올바른 파일을 찾는다")
        void pairsByQuestionIdNotAnswerOrder() {
            List<Question> questions = List.of(fileQuestion("f1", true), fileQuestion("f2", true));
            MultipartFile forF1 = file("forF1");
            MultipartFile forF2 = file("forF2");
            given(s3Service.uploadFile(forF1, UPLOAD_PATH)).willReturn("https://s3/f1.pdf");
            given(s3Service.uploadFile(forF2, UPLOAD_PATH)).willReturn("https://s3/f2.pdf");

            // questionIds/files 는 f2 가 먼저지만, 답변은 f1 이 먼저다.
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null), new AnswerInput("f2", null)),
                    List.of("f2", "f1"), List.of(forF2, forF1));

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).extracting(Answer::value)
                    .containsExactly("https://s3/f1.pdf", "https://s3/f2.pdf");
        }

        @Test
        @DisplayName("파일 질문과 일반 질문이 섞여 있어도 각각 알맞게 처리된다")
        void handlesMixedQuestionTypes() {
            List<Question> questions = List.of(textQuestion("q1"), fileQuestion("f1", true));
            MultipartFile uploaded = file("f1");
            given(s3Service.uploadFile(uploaded, UPLOAD_PATH)).willReturn("https://s3/f1.pdf");

            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("q1", "텍스트답변"), new AnswerInput("f1", null)),
                    List.of("f1"), List.of(uploaded));

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).extracting(Answer::value)
                    .containsExactly("텍스트답변", "https://s3/f1.pdf");
        }
    }

    @Nested
    @DisplayName("파일 질문 - 파일을 찾지 못하면 빈 값으로 처리한다")
    class FileFallbackToEmptyValue {

        @Test
        @DisplayName("필수가 아닌 파일 질문에 파일 입력이 아예 없으면 빈 문자열이 된다")
        void noFileInputAtAll() {
            List<Question> questions = List.of(fileQuestion("f1", false));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null)), null, null);

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).singleElement().extracting(Answer::value).isEqualTo("");
            verify(s3Service, never()).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("빈 리스트로 전달돼도 빈 문자열이 된다")
        void emptyListsAreTreatedAsNoInput() {
            List<Question> questions = List.of(fileQuestion("f1", false));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null)), List.of(), List.of());

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).singleElement().extracting(Answer::value).isEqualTo("");
            verify(s3Service, never()).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("해당 질문 ID가 questionIds에 없으면 빈 문자열이 된다")
        void questionIdNotListed() {
            List<Question> questions = List.of(fileQuestion("f1", false), fileQuestion("f2", false));
            MultipartFile onlyForF1 = file("f1");
            given(s3Service.uploadFile(onlyForF1, UPLOAD_PATH)).willReturn("https://s3/f1.pdf");

            // f2 에 대한 파일은 올라오지 않았다.
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null), new AnswerInput("f2", null)),
                    List.of("f1"), List.of(onlyForF1));

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).extracting(Answer::value)
                    .containsExactly("https://s3/f1.pdf", "");
        }

        @Test
        @DisplayName("업로드된 파일이 비어 있으면 업로드하지 않고 빈 문자열이 된다")
        void emptyFileIsNotUploaded() {
            List<Question> questions = List.of(fileQuestion("f1", false));
            MultipartFile emptyFile = new MockMultipartFile("f1", "empty.pdf", "application/pdf", new byte[0]);

            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null)),
                    List.of("f1"), List.of(emptyFile));

            List<Answer> answers = answerAssembler.assemble(submission, questions, EMAIL);

            assertThat(answers).singleElement().extracting(Answer::value).isEqualTo("");
            verify(s3Service, never()).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("questionIds에 일반 질문 ID가 섞이면 업로드 전에 거부한다")
        void rejectsNonFileQuestionIdBeforeUpload() {
            List<Question> questions = List.of(textQuestion("q1"), fileQuestion("f1", false));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("q1", "answer"), new AnswerInput("f1", null)),
                    List.of("q1", "f1"), List.of(file("q1"), file("f1")));
            assertThatThrownBy(() -> answerAssembler.assemble(submission, questions, EMAIL))
                    .isInstanceOf(InvalidAnswerException.class);
            verify(s3Service, never()).uploadFile(any(), anyString());
        }
    }

    @Nested
    @DisplayName("검증 실패")
    class ValidationFailures {

        @Test
        @DisplayName("필수 파일 질문이 있는데 파일 입력이 없으면 AnswerRequestNotMatchException")
        void requiredFileQuestionWithoutAnyFile() {
            List<Question> questions = List.of(fileQuestion("f1", true));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null)), null, null);

            assertThatThrownBy(() -> answerAssembler.assemble(submission, questions, EMAIL))
                    .isInstanceOf(AnswerRequestNotMatchException.class);
        }

        @Test
        @DisplayName("파일 질문 ID는 전달됐는데 파일이 없으면 AnswerRequestNotMatchException")
        void fileQuestionIdWithoutFiles() {
            List<Question> questions = List.of(fileQuestion("f1", false));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null)), List.of("f1"), null);

            assertThatThrownBy(() -> answerAssembler.assemble(submission, questions, EMAIL))
                    .isInstanceOf(AnswerRequestNotMatchException.class);
        }

        @Test
        @DisplayName("파일 질문 개수와 파일 개수가 다르면 ListSizeNotMatchException")
        void fileCountMismatch() {
            List<Question> questions = List.of(fileQuestion("f1", true), fileQuestion("f2", true));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("f1", null), new AnswerInput("f2", null)),
                    List.of("f1", "f2"), List.of(file("only-one")));

            assertThatThrownBy(() -> answerAssembler.assemble(submission, questions, EMAIL))
                    .isInstanceOf(ListSizeNotMatchException.class);
        }

        @Test
        @DisplayName("답변의 질문 ID가 지원폼에 없으면 QuestionParseFailException")
        void unknownQuestionId() {
            List<Question> questions = List.of(textQuestion("q1"));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("존재하지-않는-질문", "값")), null, null);

            assertThatThrownBy(() -> answerAssembler.assemble(submission, questions, EMAIL))
                    .isInstanceOf(QuestionParseFailException.class);
        }

        @Test
        @DisplayName("필수 파일 질문이 없으면 파일 입력이 없어도 통과한다")
        void optionalFileQuestionPassesWithoutFiles() {
            List<Question> questions = List.of(fileQuestion("f1", false), textQuestion("q1"));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("q1", "답변")), null, null);

            assertThat(answerAssembler.assemble(submission, questions, EMAIL)).hasSize(1);
        }

        @Test
        @DisplayName("questionIds만 전달되면 파일이 없어 거부한다")
        void rejectsQuestionIdsWithoutFiles() {
            List<Question> questions = List.of(textQuestion("q1"));
            AnswerSubmission submission = new AnswerSubmission(
                    List.of(new AnswerInput("q1", "답변")), List.of("q1"), null);

            assertThatThrownBy(() -> answerAssembler.assemble(submission, questions, EMAIL))
                    .isInstanceOf(AnswerRequestNotMatchException.class);
        }
    }
}
