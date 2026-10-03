package org.project.ttokttok.domain.memo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.memo.domain.Memo;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:controllerAuthorization;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemoApiControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtFactory jwtFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private S3Service s3Service;

    private ClubTestData clubA;
    private ClubTestData clubB;

    @BeforeEach
    void setUp() {
        clubA = prepareClubTestData("A");
        clubB = prepareClubTestData("B");
        flushAndClear();
    }

    private ClubTestData prepareClubTestData(String suffix) {
        Admin admin = persist(Admin.adminJoin("admin420" + suffix, "test-password"));
        Club club = persist(Club.builder().admin(admin).clubName("Club " + suffix)
                .clubUniv(ClubUniv.ENGINEERING).build());
        club.updateProfileImgUrl("https://example.com/" + suffix + ".png");
        ApplyForm form = persist(ApplyForm.createApplyForm(club, true,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(11), LocalDate.now().plusDays(12), 30,
                Set.of(ApplicableGrade.FIRST_GRADE), "Form " + suffix, "Subtitle", List.of()));
        Applicant applicant = Applicant.createApplicant(suffix + "@sangmyung.kr", "지원자 " + suffix,
                20, "컴퓨터공학", suffix + "@example.com", "01012345678",
                StudentStatus.ENROLLED, Grade.FIRST_GRADE, Gender.MALE, form);
        applicant.submitDocument(List.of());
        String memoId = applicant.getDocumentPhase().addMemo("기존 메모");
        persist(applicant);
        return new ClubTestData(club.getId(), applicant.getId(), memoId,
                "Bearer " + jwtFactory.generateValidToken(admin.getUsername(), Role.ROLE_ADMIN));
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, ClubTestData actor) throws Exception {
        flushAndClear();
        ResultActions result = mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, actor.adminToken()));
        flushAndClear();
        return result;
    }

    private void denied(MockHttpServletRequestBuilder request, ErrorMessage error) throws Exception {
        perform(request, clubA).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.details").value(error.getMessage()));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private record ClubTestData(String clubId, String applicantId, String memoId, String adminToken) { }

    @Test
    void createMemo_otherClub_forbidden() throws Exception {
        String url = memoUrl();

        // when & then
        denied(json(post(url), "{\"content\":\"새 메모\"}"), ErrorMessage.UNAUTHORIZED_APPLICANT_ACCESS);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertOriginalMemo();
    }

    @Test
    void createMemo_success() throws Exception {
        String url = memoUrl();

        // when & then
        String response = perform(json(post(url), "{\"content\":\"새 메모\"}"), clubB)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String memoId = objectMapper.readTree(response).get("memoId").asText();
        Memo created = entityManager.find(Memo.class, memoId);
        assertThat(created.getContent()).isEqualTo("새 메모");
        assertThat(created.getDocumentPhase().getApplicant().getId()).isEqualTo(clubB.applicantId());
        assertThat(entityManager.find(Applicant.class, clubB.applicantId()).getDocumentPhase().getMemos()).hasSize(2);
    }

    @Test
    void updateMemo_otherClub_forbidden() throws Exception {
        String url = memoUrl() + "/" + clubB.memoId();

        // when & then
        denied(json(patch(url), "{\"content\":\"수정 메모\"}"), ErrorMessage.UNAUTHORIZED_APPLICANT_ACCESS);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertOriginalMemo();
    }

    @Test
    void updateMemo_success() throws Exception {
        String url = memoUrl() + "/" + clubB.memoId();

        // when & then
        perform(json(patch(url), "{\"content\":\"수정 메모\"}"), clubB).andExpect(status().isOk());
        assertThat(entityManager.find(Memo.class, clubB.memoId()).getContent()).isEqualTo("수정 메모");
    }

    @Test
    void deleteMemo_otherClub_forbidden() throws Exception {
        String url = memoUrl() + "/" + clubB.memoId();

        // when & then
        denied(delete(url), ErrorMessage.UNAUTHORIZED_APPLICANT_ACCESS);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertOriginalMemo();
    }

    @Test
    void deleteMemo_success() throws Exception {
        String url = memoUrl() + "/" + clubB.memoId();

        // when & then
        perform(delete(url), clubB).andExpect(status().isOk());
        assertThat(entityManager.find(Memo.class, clubB.memoId())).isNull();
        assertThat(entityManager.find(Applicant.class, clubB.applicantId()).getDocumentPhase().getMemos()).isEmpty();
    }

    private String memoUrl() {
        return "/api/admin/applies/" + clubB.applicantId() + "/memos";
    }

    private void assertOriginalMemo() {
        assertThat(entityManager.find(Applicant.class, clubB.applicantId()).getDocumentPhase().getMemos())
                .singleElement().satisfies(memo -> {
                    assertThat(memo.getId()).isEqualTo(clubB.memoId());
                    assertThat(memo.getContent()).isEqualTo("기존 메모");
                });
    }

    @Test
    @DisplayName("Unauthenticated mutation returns 401 without changing data")
    void mutation_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(json(post(memoUrl()), "{\"content\":\"새 메모\"}")).andExpect(status().isUnauthorized());
        flushAndClear();
        assertOriginalMemo();
        verifyNoInteractions(emailService, fcmService, s3Service);
    }
}
