package org.project.ttokttok.domain.applicant.controller;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.jwt.JwtFactory;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.project.ttokttok.domain.applyform.domain.enums.QuestionType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:controllerAuthorization;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplicantUserApiControllerIntegrationTest {
    private static final String EMAIL = "answer422@sangmyung.kr";
    private static final String VALID_ANSWERS = """
            [{"questionId":"file","value":null},
             {"questionId":"text","value":" 지원 동기 "},
             {"questionId":"radio","value":"월요일"},
             {"questionId":"checkbox","value":["월요일","화요일"]},
             {"questionId":"optional","value":null},
             {"questionId":"optionalFile","value":null}]
            """;
    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JwtFactory jwtFactory;
    @MockitoBean private S3Service s3Service;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;
    private String clubId;
    private String formId;
    private String token;

    @BeforeEach
    void setUp() {
        entityManager.persist(User.signUp(EMAIL, "test-password", "지원자", true));
        Admin admin = Admin.adminJoin("admin422", "test-password");
        entityManager.persist(admin);
        Club club = Club.builder().admin(admin).clubName("검증 동아리").clubUniv(ClubUniv.ENGINEERING).build();
        entityManager.persist(club);
        ApplyForm form = ApplyForm.createApplyForm(club, false,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), null, null, 30,
                Set.of(ApplicableGrade.FIRST_GRADE), "지원폼", "설명", List.of(
                        question("file", FILE, true), question("text", SHORT_ANSWER, true),
                        question("radio", RADIO, true), question("checkbox", CHECKBOX, true),
                        question("optional", LONG_ANSWER, false), question("optionalFile", FILE, false)));
        entityManager.persist(form);
        clubId = club.getId();
        formId = form.getId();
        token = "Bearer " + jwtFactory.generateValidToken(EMAIL, Role.ROLE_USER);
        flushAndClear();
    }

    @Test
    void frontendMultipartRequestPersistsValuesAndOrder() throws Exception {
        when(s3Service.uploadFile(any(), eq("applicant/" + EMAIL + "/"))).thenReturn("https://s3/file.pdf");

        submit(VALID_ANSWERS, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Applicant applicant = entityManager.createQuery("select a from Applicant a", Applicant.class).getSingleResult();
        assertThat(applicant.getDocumentPhase().getAnswers()).extracting(Answer::value)
                .containsExactly("https://s3/file.pdf", " 지원 동기 ", "월요일",
                        List.of("월요일", "화요일"), null, "");
        assertThat(applicant.getDocumentPhase().getAnswers()).extracting(Answer::title)
                .containsExactly("file", "text", "radio", "checkbox", "optional", "optionalFile");
        verify(s3Service).uploadFile(any(), eq("applicant/" + EMAIL + "/"));
        verifyNoInteractions(emailService, fcmService);
    }

    static Stream<String> invalidAnswers() {
        return Stream.of(
                VALID_ANSWERS.replace("\"월요일\"},", "[\"월요일\"]},"),
                VALID_ANSWERS.replace("[\"월요일\",\"화요일\"]", "[\"수요일\"]"),
                VALID_ANSWERS.replace("[\"월요일\",\"화요일\"]", "[]"),
                VALID_ANSWERS.replace("[\"월요일\",\"화요일\"]", "[\"월요일\",\"월요일\"]"),
                VALID_ANSWERS.replace("\" 지원 동기 \"", "123"),
                VALID_ANSWERS.replace("\" 지원 동기 \"", "\" \""),
                "[{\"questionId\":\"file\",\"value\":null}]",
                VALID_ANSWERS.replace("\"optional\"", "\"text\""),
                VALID_ANSWERS.replace("\"optional\"", "\"unknown\""),
                VALID_ANSWERS.replace("{\"questionId\":\"optional\",\"value\":null}", "null"),
                "null");
    }

    @ParameterizedTest
    @MethodSource("invalidAnswers")
    void invalidAnswerPreventsUploadsAndPersistence(String answers) throws Exception {
        submit(answers, true).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
        assertNoWritesOrExternalCalls();
    }

    @Test
    void missingRequiredFileIsRejected() throws Exception {
        submit(VALID_ANSWERS, false).andExpect(status().isBadRequest());
        assertNoWritesOrExternalCalls();
    }

    @Test
    void nonFileIdInFilePartsIsRejected() throws Exception {
        perform(request(VALID_ANSWERS).file(jsonPart("questionIds", "[\"text\"]"))
                .file(filePart())).andExpect(status().isBadRequest());
        assertNoWritesOrExternalCalls();
    }

    private ResultActions submit(String answers, boolean withFile) throws Exception {
        MockMultipartHttpServletRequestBuilder request = request(answers);
        if (withFile) {
            request.file(jsonPart("questionIds", "[\"file\"]")).file(filePart());
        }
        return perform(request);
    }

    private MockMultipartHttpServletRequestBuilder request(String answers) {
        String body = """
                {"name":"지원자","age":20,"major":"컴퓨터공학","email":"%s",
                 "phone":"010-1234-5678","studentStatus":"ENROLLED","grade":"FIRST_GRADE",
                 "gender":"MALE","applyFormId":"%s","answers":%s}
                """.formatted(EMAIL, formId, answers);
        return multipart("/api/user/applies/{clubId}", clubId).file(jsonPart("request", body));
    }

    private ResultActions perform(MockMultipartHttpServletRequestBuilder request) throws Exception {
        ResultActions result = mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, token));
        flushAndClear();
        return result;
    }

    private void assertNoWritesOrExternalCalls() {
        assertThat(entityManager.createQuery("select count(a) from Applicant a", Long.class).getSingleResult()).isZero();
        assertThat(entityManager.createQuery("select count(d) from DocumentPhase d", Long.class).getSingleResult()).isZero();
        verifyNoInteractions(s3Service, emailService, fcmService);
    }

    private MockMultipartFile jsonPart(String name, String value) {
        return new MockMultipartFile(name, "", MediaType.APPLICATION_JSON_VALUE, value.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile filePart() {
        return new MockMultipartFile("files", "resume.pdf", "application/pdf", new byte[]{1});
    }

    private Question question(String id, org.project.ttokttok.domain.applyform.domain.enums.QuestionType type,
                              boolean essential) {
        return new Question(id, id, "설명", type, essential, List.of("월요일", "화요일"));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
