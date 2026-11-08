package org.project.ttokttok.domain.applyform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:applyFormUpdateAuthorization;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplyFormUpdateAuthorizationIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JwtFactory jwtFactory;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;
    @MockitoBean private S3Service s3Service;

    private String formIdOfClubB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        createClub("A");
        Club clubB = createClub("B");
        ApplyForm form = ApplyForm.createApplyForm(clubB, false, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), null, null, 10, Set.of(ApplicableGrade.FIRST_GRADE),
                "original", "subtitle", List.of(new Question("q1", "지원 동기", null,
                        QuestionType.LONG_ANSWER, true, List.of())));
        entityManager.persist(form);
        formIdOfClubB = form.getId();
        tokenA = token("formAdminA");
        tokenB = token("formAdminB");
        flushAndClear();
    }

    private Club createClub(String suffix) {
        Admin admin = Admin.adminJoin("formAdmin" + suffix, "test-password");
        entityManager.persist(admin);
        Club club = Club.builder().admin(admin).clubName("Form club " + suffix)
                .clubUniv(ClubUniv.ENGINEERING).build();
        entityManager.persist(club);
        return club;
    }

    private String token(String username) {
        return "Bearer " + jwtFactory.generateValidToken(username, Role.ROLE_ADMIN);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private ResultActions update(String formId, String token) throws Exception {
        ResultActions result = mockMvc.perform(patch("/api/admin/forms/" + formId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .content("""
                        {"title":"changed","subTitle":"changed","questions":[]}
                        """));
        flushAndClear();
        return result;
    }

    private ApplyForm reload() {
        return entityManager.find(ApplyForm.class, formIdOfClubB);
    }

    @Test
    void otherClubAdmin_forbidden_preservesForm() throws Exception {
        update(formIdOfClubB, tokenA).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.details").value(ErrorMessage.NOT_CLUB_ADMIN.getMessage()));
        ApplyForm unchanged = reload();
        assertThat(unchanged.getTitle()).isEqualTo("original");
        assertThat(unchanged.getSubTitle()).isEqualTo("subtitle");
        assertThat(unchanged.getFormJson()).extracting(Question::questionId).containsExactly("q1");
    }

    @Test
    void owner_updatesForm() throws Exception {
        update(formIdOfClubB, tokenB).andExpect(status().isOk());
        ApplyForm updated = reload();
        assertThat(updated.getTitle()).isEqualTo("changed");
        assertThat(updated.getSubTitle()).isEqualTo("changed");
        assertThat(updated.getFormJson()).isEmpty();
    }

    @Test
    void missingForm_notFound() throws Exception {
        update("missing-form", tokenA).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details").value(ErrorMessage.APPLY_FORM_NOT_FOUND.getMessage()));
    }
}
