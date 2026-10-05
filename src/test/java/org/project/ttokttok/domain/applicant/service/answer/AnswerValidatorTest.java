package org.project.ttokttok.domain.applicant.service.answer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.project.ttokttok.domain.applicant.exception.AnswerRequestNotMatchException;
import org.project.ttokttok.domain.applicant.exception.InvalidAnswerException;
import org.project.ttokttok.domain.applicant.exception.ListSizeNotMatchException;
import org.project.ttokttok.domain.applicant.exception.QuestionParseFailException;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.project.ttokttok.domain.applyform.domain.enums.QuestionType.*;

class AnswerValidatorTest {
    private final AnswerValidator validator = new AnswerValidator();

    static Stream<Arguments> validValues() {
        return Stream.of(
                Arguments.of(SHORT_ANSWER, true, " text "), Arguments.of(LONG_ANSWER, true, "text"),
                Arguments.of(RADIO, true, "월요일"), Arguments.of(CHECKBOX, true, List.of("월요일", "화요일")),
                Arguments.of(SHORT_ANSWER, false, null), Arguments.of(LONG_ANSWER, false, " "),
                Arguments.of(RADIO, false, null), Arguments.of(CHECKBOX, false, null),
                Arguments.of(CHECKBOX, false, List.of()), Arguments.of(FILE, false, null));
    }

    @ParameterizedTest(name = "{0} 필수={1}, 값={2}")
    @MethodSource("validValues")
    void acceptsFrontendValues(QuestionType type, boolean essential, Object value) {
        assertThatCode(() -> validate(type, essential, value)).doesNotThrowAnyException();
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of(SHORT_ANSWER, 1), Arguments.of(LONG_ANSWER, List.of("text")),
                Arguments.of(RADIO, List.of("월요일")), Arguments.of(RADIO, "수요일"),
                Arguments.of(CHECKBOX, "월요일"), Arguments.of(CHECKBOX, List.of(1)),
                Arguments.of(CHECKBOX, List.of("수요일")),
                Arguments.of(CHECKBOX, List.of("월요일", "월요일")),
                Arguments.of(CHECKBOX, Arrays.asList("월요일", null)),
                Arguments.of(FILE, "https://client/file"));
    }

    @ParameterizedTest(name = "{0} 잘못된 값={1}")
    @MethodSource("invalidValues")
    void rejectsWrongTypesAndChoices(QuestionType type, Object value) {
        assertThatThrownBy(() -> validate(type, false, value)).isInstanceOf(InvalidAnswerException.class);
    }

    static Stream<Arguments> missingValues() {
        return Stream.of(Arguments.of(SHORT_ANSWER, null), Arguments.of(SHORT_ANSWER, " "),
                Arguments.of(LONG_ANSWER, "\t"), Arguments.of(RADIO, null), Arguments.of(CHECKBOX, null));
    }

    @ParameterizedTest
    @MethodSource("missingValues")
    void preservesRequiredAnswerError(QuestionType type, Object value) {
        assertThatThrownBy(() -> validate(type, true, value)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 질문에 대한 답변이 없습니다.");
    }

    @Test
    void rejectsEmptyRequiredCheckbox() {
        assertThatThrownBy(() -> validate(CHECKBOX, true, List.of())).isInstanceOf(InvalidAnswerException.class);
    }

    static Stream<Arguments> invalidAnswerLists() {
        return Stream.of(Arguments.of(Arrays.asList((AnswerInput) null)),
                Arguments.of(List.of(new AnswerInput(null, "text"))),
                Arguments.of(List.of(new AnswerInput(" ", "text"))),
                Arguments.of(List.of(new AnswerInput("q", "text"), new AnswerInput("q", "text"))));
    }

    @ParameterizedTest
    @MethodSource("invalidAnswerLists")
    void rejectsMalformedOrDuplicateAnswers(List<AnswerInput> answers) {
        assertThatThrownBy(() -> validator.validate(new AnswerSubmission(answers, null, null),
                List.of(question("q", SHORT_ANSWER, false)))).isInstanceOf(InvalidAnswerException.class);
    }

    @Test
    void rejectsUnknownQuestion() {
        assertThatThrownBy(() -> validator.validate(
                new AnswerSubmission(List.of(new AnswerInput("unknown", "text")), null, null),
                List.of(question("q", SHORT_ANSWER, false)))).isInstanceOf(QuestionParseFailException.class);
    }

    @Test
    void nullAnswersAreAllowedOnlyWithoutRequiredQuestions() {
        AnswerSubmission submission = new AnswerSubmission(null, null, null);
        assertThatCode(() -> validator.validate(submission, List.of(question("q", SHORT_ANSWER, false))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(submission, List.of(question("q", SHORT_ANSWER, true))))
                .isInstanceOf(InvalidAnswerException.class);
    }

    static Stream<Arguments> invalidFilePairs() {
        MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", new byte[]{1});
        List<AnswerInput> answers = List.of(new AnswerInput("f", null), new AnswerInput("q", "text"));
        return Stream.of(
                Arguments.of(new AnswerSubmission(answers, null, List.of(file)), AnswerRequestNotMatchException.class),
                Arguments.of(new AnswerSubmission(answers, List.of("f"), null), AnswerRequestNotMatchException.class),
                Arguments.of(new AnswerSubmission(answers, List.of("f", "q"), List.of(file)), ListSizeNotMatchException.class),
                Arguments.of(new AnswerSubmission(answers, List.of("f", "f"), List.of(file, file)), InvalidAnswerException.class),
                Arguments.of(new AnswerSubmission(answers, List.of("q"), List.of(file)), InvalidAnswerException.class),
                Arguments.of(new AnswerSubmission(answers, List.of("unknown"), List.of(file)), InvalidAnswerException.class),
                Arguments.of(new AnswerSubmission(answers, Arrays.asList((String) null), List.of(file)), InvalidAnswerException.class),
                Arguments.of(new AnswerSubmission(answers, List.of(" "), List.of(file)), InvalidAnswerException.class),
                Arguments.of(new AnswerSubmission(List.of(), List.of("f"), List.of(file)), InvalidAnswerException.class));
    }

    @ParameterizedTest
    @MethodSource("invalidFilePairs")
    void rejectsInvalidFilePairs(AnswerSubmission submission, Class<? extends Throwable> error) {
        assertThatThrownBy(() -> validator.validate(submission,
                List.of(question("f", FILE, false), question("q", SHORT_ANSWER, false))))
                .isInstanceOf(error);
    }

    @Test
    void requiredFileMustExistAndBeNonempty() {
        Question question = question("f", FILE, true);
        for (AnswerSubmission submission : List.of(
                new AnswerSubmission(List.of(), null, null),
                new AnswerSubmission(List.of(new AnswerInput("f", null)), null, null),
                new AnswerSubmission(List.of(new AnswerInput("f", null)), List.of("f"),
                        List.of(new MockMultipartFile("files", new byte[0]))),
                new AnswerSubmission(List.of(new AnswerInput("f", null)), List.of("f"),
                        Arrays.asList((org.springframework.web.multipart.MultipartFile) null)))) {
            assertThatThrownBy(() -> validator.validate(submission, List.of(question)))
                    .isInstanceOf(AnswerRequestNotMatchException.class);
        }
    }

    private void validate(QuestionType type, boolean essential, Object value) {
        validator.validate(new AnswerSubmission(List.of(new AnswerInput("q", value)), null, null),
                List.of(question("q", type, essential)));
    }

    private static Question question(String id, QuestionType type, boolean essential) {
        return new Question(id, "질문", "설명", type, essential, List.of("월요일", "화요일"));
    }
}
