package org.project.ttokttok.domain.applyform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:applyFormEditLock;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplyFormEditLockIntegrationTest {
    private static final String Q1 = "q1";
    private static final String Q2 = "q2";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JwtFactory jwtFactory;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;
    @MockitoBean private S3Service s3Service;

    private Setup createClubWithForm(String suffix, boolean withApplicant) {
        Admin admin = Admin.adminJoin("lockAdmin" + suffix, "test-password");
        entityManager.persist(admin);
        Club club = Club.builder().admin(admin).clubName("Lock club " + suffix)
                .clubUniv(ClubUniv.ENGINEERING).build();
        entityManager.persist(club);
        ApplyForm form = ApplyForm.createApplyForm(club, false, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), null, null, 10, Set.of(ApplicableGrade.FIRST_GRADE),
                "original", "subtitle", List.of(
                        new Question(Q1, "지원 동기", null, QuestionType.LONG_ANSWER, true, List.of("")),
                        new Question(Q2, "가능 요일", null, QuestionType.CHECKBOX, false, List.of("월", "화"))));
        entityManager.persist(form);
        if (withApplicant) {
            entityManager.persist(Applicant.createApplicant("lock" + suffix + "@sangmyung.kr", "지원자", 20, "컴퓨터",
                    "a@a.com", "010", StudentStatus.ENROLLED, Grade.FIRST_GRADE, Gender.MALE, form));
        }
        entityManager.flush();
        entityManager.clear();
        String token = "Bearer " + jwtFactory.generateValidToken("lockAdmin" + suffix, Role.ROLE_ADMIN);
        return new Setup(club.getId(), form.getId(), token);
    }

    private ResultActions patchQuestions(Setup setup, String questionsJson) throws Exception {
        ResultActions result = mockMvc.perform(patch("/api/admin/forms/" + setup.formId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, setup.token())
                .content("{\"questions\":" + questionsJson + "}"));
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private static final String Q1_JSON = """
            {"questionId":"q1","title":"지원 동기","questionType":"LONG_ANSWER","isEssential":true,"content":[""]}""";
    private static final String Q2_JSON = """
            {"questionId":"q2","title":"가능 요일","questionType":"CHECKBOX","isEssential":false,"content":["월","화"]}""";
    private static final String NEW_OPTIONAL_JSON = """
            {"title":"특기","questionType":"SHORT_ANSWER","isEssential":false,"content":[""]}""";

    @Test
    void 지원자가_있으면_질문_삭제는_409() throws Exception {
        Setup setup = createClubWithForm("A", true);

        patchQuestions(setup, "[" + Q2_JSON + "]").andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").value(
                        "지원서가 제출된 폼이라 '지원 동기' 질문을 삭제할 수 없습니다."));

        assertThat(entityManager.find(ApplyForm.class, setup.formId()).getFormJson())
                .extracting(Question::questionId).containsExactly(Q1, Q2);
    }

    @Test
    void 지원자가_있어도_선택_질문_추가는_200() throws Exception {
        Setup setup = createClubWithForm("B", true);

        patchQuestions(setup, "[" + Q1_JSON + "," + Q2_JSON + "," + NEW_OPTIONAL_JSON + "]")
                .andExpect(status().isOk());

        assertThat(entityManager.find(ApplyForm.class, setup.formId()).getFormJson()).hasSize(3);
    }

    @Test
    void 지원자가_없으면_질문_삭제도_200() throws Exception {
        Setup setup = createClubWithForm("C", false);

        patchQuestions(setup, "[" + Q2_JSON + "]").andExpect(status().isOk());

        assertThat(entityManager.find(ApplyForm.class, setup.formId()).getFormJson())
                .extracting(Question::questionId).containsExactly(Q2);
    }

    @Test
    void 지원자가_있어도_questions_를_보내지_않으면_200() throws Exception {
        Setup setup = createClubWithForm("D", true);

        mockMvc.perform(patch("/api/admin/forms/" + setup.formId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, setup.token())
                        .content("{\"title\":\"changed\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 상세조회는_지원자가_있으면_hasApplicants_true() throws Exception {
        Setup setup = createClubWithForm("E", true);

        mockMvc.perform(get("/api/admin/forms/" + setup.clubId())
                        .header(HttpHeaders.AUTHORIZATION, setup.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formId").value(setup.formId()))
                .andExpect(jsonPath("$.hasApplicants").value(true));
    }

    @Test
    void 상세조회는_지원자가_없으면_hasApplicants_false() throws Exception {
        Setup setup = createClubWithForm("F", false);

        mockMvc.perform(get("/api/admin/forms/" + setup.clubId())
                        .header(HttpHeaders.AUTHORIZATION, setup.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasApplicants").value(false));
    }

    @Test
    void 활성_폼이_없으면_hasApplicants_false() throws Exception {
        Setup setup = createClubWithForm("G", true);
        entityManager.find(ApplyForm.class, setup.formId()).finish();
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/admin/forms/" + setup.clubId())
                        .header(HttpHeaders.AUTHORIZATION, setup.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasApplicants").value(false));
    }

    private record Setup(String clubId, String formId, String token) {
    }
}
