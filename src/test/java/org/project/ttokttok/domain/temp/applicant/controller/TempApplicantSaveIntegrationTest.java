package org.project.ttokttok.domain.temp.applicant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.RecruitStatePolicy;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.global.exception.ErrorMessage;
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

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:tempApplicantSave;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TempApplicantSaveIntegrationTest {
    private static final String EMAIL = "tempUser@sangmyung.kr";
    private static final String OLD_URL = "https://cdn.test/temp-applicants/" + EMAIL + "/old_resume.pdf";
    private static final String NEW_URL = "https://cdn.test/temp-applicants/" + EMAIL + "/new_resume.pdf";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JwtFactory jwtFactory;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;
    @MockitoBean private S3Service s3Service;

    private Club club;
    private String token;

    @BeforeEach
    void setUp() {
        Admin admin = Admin.adminJoin("tempAdmin", "test-password");
        entityManager.persist(admin);
        club = Club.builder().admin(admin).clubName("Temp club").clubUniv(ClubUniv.ENGINEERING).build();
        entityManager.persist(club);
        entityManager.persist(User.signUp(EMAIL, "encoded-password", "임시유저", true));
        token = "Bearer " + jwtFactory.generateValidToken(EMAIL, Role.ROLE_USER);
        given(s3Service.uploadFile(any(), anyString())).willReturn(NEW_URL);
    }

    private String form(int startOffsetDays, int endOffsetDays) {
        LocalDate today = RecruitStatePolicy.today();
        ApplyForm form = ApplyForm.createApplyForm(club, false, today.plusDays(startOffsetDays),
                today.plusDays(endOffsetDays), null, null, 10, Set.of(ApplicableGrade.FIRST_GRADE),
                "title", "subtitle", List.of(new Question("q1", "지원 동기", null,
                        QuestionType.LONG_ANSWER, true, List.of())));
        entityManager.persist(form);
        entityManager.flush();
        return form.getId();
    }

    private String openForm() {
        return form(-1, 5);
    }

    private ResultActions save(String formId, String requestJson, List<String> questionIds,
                               List<MockMultipartFile> files) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/user/temp-applicant/" + formId)
                .file(new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE,
                        requestJson.getBytes()));
        builder.header(HttpHeaders.AUTHORIZATION, token);
        if (!questionIds.isEmpty()) {
            String json = questionIds.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
            builder.file(new MockMultipartFile("questionIds", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes()));
        }
        for (MockMultipartFile file : files) {
            builder.file(file);
        }
        ResultActions result = mockMvc.perform(builder);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private ResultActions save(String formId, String requestJson) throws Exception {
        return save(formId, requestJson, List.of(), List.of());
    }

    private static String draft(String answersJson) {
        return "{\"name\":\"\",\"answers\":" + answersJson + "}";
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("files", "resume.pdf", "application/pdf", "content".getBytes());
    }

    private long draftCount(String formId) {
        return entityManager.createQuery(
                        "select count(t) from TempApplicant t where t.formId = :f and t.userEmail = :e", Long.class)
                .setParameter("f", formId).setParameter("e", EMAIL).getSingleResult();
    }

    @Test
    void missingForm_notFound() throws Exception {
        save("missing-form", draft("[]")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details").value(ErrorMessage.APPLY_FORM_NOT_FOUND.getMessage()));
    }

    @Test
    void closedForm_conflict_notSaved() throws Exception {
        String formId = form(-10, -1);
        save(formId, draft("[]")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").value(ErrorMessage.TEMP_APPLY_NOT_OPEN.getMessage()));
        assertThat(draftCount(formId)).isZero();
    }

    @Test
    void upcomingForm_conflict_notSaved() throws Exception {
        String formId = form(3, 10);
        save(formId, draft("[]")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").value(ErrorMessage.TEMP_APPLY_NOT_OPEN.getMessage()));
        assertThat(draftCount(formId)).isZero();
    }

    @Test
    void openForm_savesIncompleteDraft_withBlankAnswerAndNoGrade() throws Exception {
        String formId = openForm();
        save(formId, draft("[{\"questionId\":\"q1\",\"value\":\"\"}]")).andExpect(status().isCreated());
        assertThat(draftCount(formId)).isEqualTo(1);
    }

    @Test
    void secondSave_updatesSameRow() throws Exception {
        String formId = openForm();
        save(formId, draft("[{\"questionId\":\"q1\",\"value\":\"first\"}]")).andExpect(status().isCreated());
        save(formId, draft("[{\"questionId\":\"q1\",\"value\":\"second\"}]")).andExpect(status().isCreated());

        assertThat(draftCount(formId)).isEqualTo(1);
        mockMvc.perform(get("/api/user/temp-applicant/" + formId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasTempData").value(true))
                .andExpect(jsonPath("$.data.answers[0].value").value("second"));
    }

    @Test
    void missingAnswers_badRequest() throws Exception {
        save(openForm(), "{\"name\":\"n\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value(ErrorMessage.ANSWER_REQUEST_NOT_MATCH.getMessage()));
    }

    @Test
    void answerWithoutQuestionId_badRequest() throws Exception {
        save(openForm(), draft("[{\"value\":\"x\"}]")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value(ErrorMessage.INVALID_ANSWER.getMessage()));
    }

    @Test
    void fileWithoutQuestionIds_badRequest_noUpload() throws Exception {
        save(openForm(), draft("[]"), List.of(), List.of(pdf())).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value(ErrorMessage.ANSWER_REQUEST_NOT_MATCH.getMessage()));
        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    void questionIdsWithoutFiles_badRequest() throws Exception {
        save(openForm(), draft("[]"), List.of("q-file"), List.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value(ErrorMessage.ANSWER_REQUEST_NOT_MATCH.getMessage()));
    }

    @Test
    void fileCountMismatch_badRequest_noUpload() throws Exception {
        save(openForm(), draft("[]"), List.of("q-file1", "q-file2"), List.of(pdf())).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value(ErrorMessage.LIST_SIZE_NOT_MATCH.getMessage()));
        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    void replacedTempFile_isDeletedAfterCommit_andKeptFileIsNot() throws Exception {
        String formId = openForm();
        String keptUrl = "https://cdn.test/temp-applicants/" + EMAIL + "/kept.pdf";
        save(formId, draft("[{\"questionId\":\"q-old\",\"value\":\"" + OLD_URL + "\"},"
                + "{\"questionId\":\"q-kept\",\"value\":\"" + keptUrl + "\"}]")).andExpect(status().isCreated());

        save(formId, draft("[{\"questionId\":\"q-kept\",\"value\":\"" + keptUrl + "\"}]"),
                List.of("q-new"), List.of(pdf())).andExpect(status().isCreated());

        verify(s3Service).deleteFileAfterCommit(OLD_URL);
        verify(s3Service, never()).deleteFileAfterCommit(keptUrl);
        verify(s3Service, never()).deleteFileAfterCommit(NEW_URL);
        assertThat(draftCount(formId)).isEqualTo(1);
    }
}
