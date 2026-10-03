package org.project.ttokttok.domain.applicant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.controller.dto.request.MailFormatRequest;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
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
class ApplicantAdminApiControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtFactory jwtFactory;

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
        return new ClubTestData(club.getId(), form.getId(), applicant.getId(), memoId,
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

    private record ClubTestData(String clubId, String formId, String applicantId, String memoId, String adminToken) { }

    @Test
    void getDetail_otherClub_forbidden() throws Exception {
        String url = "/api/admin/applies/" + clubB.applicantId();

        // when & then
        denied(get(url), ErrorMessage.UNAUTHORIZED_APPLICANT_ACCESS);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertThat(entityManager.find(Applicant.class, clubB.applicantId()).getDocumentPhase().getStatus())
                .isEqualTo(PhaseStatus.EVALUATING);
    }

    @Test
    void getDetail_success() throws Exception {
        String url = "/api/admin/applies/" + clubB.applicantId();

        // when & then
        perform(get(url), clubB).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("지원자 B"))
                .andExpect(jsonPath("$.email").value("B@example.com"))
                .andExpect(jsonPath("$.memos[0].content").value("기존 메모"));
    }

    @Test
    void updateEvaluation_otherClub_forbidden() throws Exception {
        String url = "/api/admin/applies/evaluations/" + clubB.applicantId();

        // when & then
        denied(json(patch(url).param("kind", "DOCUMENT"), "{\"status\":\"PASS\"}"),
                ErrorMessage.UNAUTHORIZED_APPLICANT_ACCESS);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertThat(entityManager.find(Applicant.class, clubB.applicantId()).getDocumentPhase().getStatus())
                .isEqualTo(PhaseStatus.EVALUATING);
    }

    @Test
    void updateEvaluation_success() throws Exception {
        String url = "/api/admin/applies/evaluations/" + clubB.applicantId();

        // when & then
        perform(json(patch(url).param("kind", "DOCUMENT"), "{\"status\":\"PASS\"}"), clubB)
                .andExpect(status().isOk());
        assertThat(entityManager.find(Applicant.class, clubB.applicantId()).getDocumentPhase().getStatus())
                .isEqualTo(PhaseStatus.PASS);
    }

    @Test
    void finalize_otherClub_forbidden() throws Exception {
        Applicant applicant = entityManager.find(Applicant.class, clubB.applicantId());
        applicant.passDocumentEvaluation();
        applicant.updateToInterviewPhase(LocalDate.now().plusDays(11));
        applicant.passInterview();
        flushAndClear();
        long beforeCount = memberCount();
        String url = "/api/admin/applies/" + clubB.clubId() + "/finalize";

        // when & then
        denied(put(url).param("kind", "INTERVIEW"), ErrorMessage.NOT_CLUB_ADMIN);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertThat(memberCount()).isEqualTo(beforeCount);
        Applicant unchanged = entityManager.find(Applicant.class, clubB.applicantId());
        assertThat(unchanged.getCurrentPhase()).isEqualTo(ApplicantPhase.INTERVIEW);
        assertThat(unchanged.getDocumentPhase().getStatus()).isEqualTo(PhaseStatus.PASS);
        assertThat(unchanged.getInterviewPhase().getStatus()).isEqualTo(PhaseStatus.PASS);
    }

    @Test
    void finalize_success() throws Exception {
        Applicant applicant = entityManager.find(Applicant.class, clubB.applicantId());
        applicant.passDocumentEvaluation();
        applicant.updateToInterviewPhase(LocalDate.now().plusDays(11));
        applicant.passInterview();
        flushAndClear();
        long beforeCount = memberCount();
        String url = "/api/admin/applies/" + clubB.clubId() + "/finalize";

        // when & then
        perform(put(url).param("kind", "INTERVIEW"), clubB).andExpect(status().isOk());
        assertThat(memberCount()).isEqualTo(beforeCount + 1);
        List<ClubMember> members = entityManager.createQuery(
                "select m from ClubMember m where m.club.id = :clubId and m.email = :email", ClubMember.class)
                .setParameter("clubId", clubB.clubId()).setParameter("email", "B@example.com").getResultList();
        assertThat(members).singleElement().satisfies(member -> {
            assertThat(member.getRole()).isEqualTo(MemberRole.MEMBER);
            assertThat(member.getMemberName()).isEqualTo("지원자 B");
            assertThat(member.getClub().getId()).isEqualTo(clubB.clubId());
        });
    }

    @Test
    void sendMail_otherClub_forbidden() throws Exception {
        entityManager.find(Applicant.class, clubB.applicantId()).passDocumentEvaluation();
        Applicant failed = Applicant.createApplicant("failed@sangmyung.kr", "불합격자", 20, "컴퓨터공학",
                "failed@example.com", "01012345678", StudentStatus.ENROLLED, Grade.FIRST_GRADE,
                Gender.MALE, entityManager.find(ApplyForm.class, clubB.formId()));
        failed.submitDocument(List.of());
        failed.failDocumentEvaluation();
        persist(failed);
        flushAndClear();
        long beforeCount = memberCount();
        String url = "/api/admin/applies/" + clubB.clubId() + "/send-email";
        String body = """
                {"pass":{"title":"합격","body":"축하합니다"},"fail":{"title":"불합격","body":"다음에 만나요"}}
                """;

        // when & then
        denied(json(post(url).param("kind", "DOCUMENT"), body), ErrorMessage.NOT_CLUB_ADMIN);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertThat(entityManager.find(Applicant.class, clubB.applicantId()).getDocumentPhase().getStatus())
                .isEqualTo(PhaseStatus.PASS);
        assertThat(entityManager.find(Applicant.class, failed.getId()).getDocumentPhase().getStatus())
                .isEqualTo(PhaseStatus.FAIL);
        assertThat(memberCount()).isEqualTo(beforeCount);
    }

    @Test
    void sendMail_success() throws Exception {
        entityManager.find(Applicant.class, clubB.applicantId()).passDocumentEvaluation();
        Applicant failed = Applicant.createApplicant("failed@sangmyung.kr", "불합격자", 20, "컴퓨터공학",
                "failed@example.com", "01012345678", StudentStatus.ENROLLED, Grade.FIRST_GRADE,
                Gender.MALE, entityManager.find(ApplyForm.class, clubB.formId()));
        failed.submitDocument(List.of());
        failed.failDocumentEvaluation();
        persist(failed);
        flushAndClear();
        String url = "/api/admin/applies/" + clubB.clubId() + "/send-email";
        String body = """
                {"pass":{"title":"합격","body":"축하합니다"},"fail":{"title":"불합격","body":"다음에 만나요"}}
                """;

        // when & then
        perform(json(post(url).param("kind", "DOCUMENT"), body), clubB).andExpect(status().isOk());
        verify(emailService).sendResultMail(List.of("B@example.com"), new MailFormatRequest("합격", "축하합니다"));
        verify(emailService).sendResultMail(List.of("failed@example.com"), new MailFormatRequest("불합격", "다음에 만나요"));
        verifyNoInteractions(fcmService, s3Service);
    }

    private long memberCount() {
        return entityManager.createQuery("select count(m) from ClubMember m", Long.class).getSingleResult();
    }

    @Test
    @DisplayName("Unauthenticated mutation returns 401 without changing data")
    void mutation_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(json(patch("/api/admin/applies/evaluations/" + clubB.applicantId()).param("kind", "DOCUMENT"), "{\"status\":\"PASS\"}")).andExpect(status().isUnauthorized());
        flushAndClear();
        assertThat(entityManager.find(Applicant.class, clubB.applicantId()).getDocumentPhase().getStatus()).isEqualTo(PhaseStatus.EVALUATING);
        verifyNoInteractions(emailService, fcmService, s3Service);
    }
}
