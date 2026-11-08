package org.project.ttokttok.domain.applyform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:finishEvaluation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FinishEvaluationIntegrationTest {
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
        Admin admin = Admin.adminJoin("finishAdmin", "test-password");
        entityManager.persist(admin);
        club = Club.builder().admin(admin).clubName("Finish club").clubUniv(ClubUniv.ENGINEERING).build();
        entityManager.persist(club);
        token = "Bearer " + jwtFactory.generateValidToken("finishAdmin", Role.ROLE_ADMIN);
    }

    private ApplyForm persistForm(boolean recruiting) {
        ApplyForm form = ApplyForm.createApplyForm(club, false, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), null, null, 10, Set.of(ApplicableGrade.FIRST_GRADE),
                "form", "subtitle", List.of(new Question("q1", "지원 동기", null,
                        QuestionType.LONG_ANSWER, false, List.of())));
        if (!recruiting) {
            form.endRecruiting();
        }
        entityManager.persist(form);
        flushAndClear();
        return form;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private ResultActions finish(String formId) throws Exception {
        ResultActions result = mockMvc.perform(post("/api/admin/forms/finish/" + formId)
                .header(HttpHeaders.AUTHORIZATION, token));
        flushAndClear();
        return result;
    }

    private ApplyFormStatus statusOf(String formId) {
        return entityManager.find(ApplyForm.class, formId).getStatus();
    }

    @Test
    void 평가_종료를_두_번_요청해도_종료_상태로_남는다() throws Exception {
        // given
        String formId = persistForm(false).getId();

        // when
        finish(formId).andExpect(status().isOk());
        finish(formId).andExpect(status().isOk());

        // then
        assertThat(statusOf(formId)).isEqualTo(ApplyFormStatus.INACTIVE);
    }
}
