package org.project.ttokttok.domain.club.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory.PROFILE_IMAGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.project.ttokttok.domain.notification.fcm.domain.DeviceType;
import org.project.ttokttok.domain.notification.fcm.domain.FCMToken;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.firebase.service.dto.FCMRequest;
import org.project.ttokttok.infrastructure.jwt.JwtFactory;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:controllerAuthorization;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClubAdminApiControllerTest {
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
    private static final String NEW_IMAGE = "https://example.com/new.png";

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
        User user = persist(User.signUp(suffix + "@sangmyung.kr", "test-password", "사용자 " + suffix, true));
        persist(Favorite.create(user, club));
        persist(FCMToken.create(DeviceType.WEB, user.getEmail(), "token-" + suffix));
        return new ClubTestData(club.getId(), form.getId(),
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

    private MockMultipartHttpServletRequestBuilder imageRequest(String method, String url, String body, String imagePart) {
        return multipart(HttpMethod.valueOf(method), url)
                .file(new MockMultipartFile("request", "request.json", "application/json", body.getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile(imagePart, "image.png", "image/png", Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")));
    }

    private record ClubTestData(String clubId, String formId, String adminToken) { }

    @Test
    void updateContent_otherClub_forbidden() throws Exception {
        String url = "/api/admin/clubs/" + clubB.clubId() + "/content";
        String body = "{\"name\":\"수정 동아리\",\"summary\":\"수정 소개\",\"content\":\"수정 내용\"}";
        Club original = entityManager.find(Club.class, clubB.clubId());
        String originalSummary = original.getSummary();
        String originalContent = original.getContent();

        // when & then
        denied(imageRequest("PATCH", url, body, "profileImage"), ErrorMessage.NOT_CLUB_ADMIN);
        verifyNoInteractions(emailService, fcmService, s3Service);
        Club unchanged = entityManager.find(Club.class, clubB.clubId());
        assertThat(unchanged.getName()).isEqualTo("Club B");
        assertThat(unchanged.getSummary()).isEqualTo(originalSummary);
        assertThat(unchanged.getContent()).isEqualTo(originalContent);
        assertThat(unchanged.getProfileImageUrl()).isEqualTo("https://example.com/B.png");
    }

    @Test
    void updateContent_success() throws Exception {
        String url = "/api/admin/clubs/" + clubB.clubId() + "/content";
        String body = "{\"name\":\"수정 동아리\",\"summary\":\"수정 소개\",\"content\":\"수정 내용\"}";

        // when & then
        when(s3Service.uploadFile(any(), anyString())).thenReturn(NEW_IMAGE);
        perform(imageRequest("PATCH", url, body, "profileImage"), clubB).andExpect(status().isOk());
        Club updated = entityManager.find(Club.class, clubB.clubId());
        assertThat(updated.getName()).isEqualTo("수정 동아리");
        assertThat(updated.getSummary()).isEqualTo("수정 소개");
        assertThat(updated.getContent()).isEqualTo("수정 내용");
        assertThat(updated.getProfileImageUrl()).isEqualTo(NEW_IMAGE);
        verify(s3Service).uploadFile(any(), eq(PROFILE_IMAGE.getDirectoryName()));
        verify(s3Service).deleteFile("https://example.com/B.png");
        verifyNoInteractions(emailService, fcmService);
    }

    @ParameterizedTest(name = "모집 변경: 변경 전 모집 중={0}")
    @ValueSource(booleans = {false, true})
    void toggleRecruitment_otherClub_forbidden(boolean recruiting) throws Exception {
        if (!recruiting) {
            entityManager.find(ApplyForm.class, clubB.formId()).endRecruiting();
        }
        String url = "/api/admin/clubs/" + clubB.clubId() + "/toggle-recruitment";
        denied(patch(url), ErrorMessage.NOT_CLUB_ADMIN);
        verifyNoInteractions(emailService, fcmService, s3Service);
        ApplyForm unchanged = entityManager.find(ApplyForm.class, clubB.formId());
        assertThat(unchanged.getStatus()).isEqualTo(ApplyFormStatus.ACTIVE);
        assertThat(unchanged.isRecruiting()).isEqualTo(recruiting);
    }

    @ParameterizedTest(name = "모집 변경: 변경 전 모집 중={0}")
    @ValueSource(booleans = {false, true})
    void toggleRecruitment_success(boolean recruiting) throws Exception {
        if (!recruiting) {
            entityManager.find(ApplyForm.class, clubB.formId()).endRecruiting();
        }
        String url = "/api/admin/clubs/" + clubB.clubId() + "/toggle-recruitment";
        perform(patch(url), clubB).andExpect(status().isOk());
        ApplyForm updated = entityManager.find(ApplyForm.class, clubB.formId());
        assertThat(updated.getStatus()).isEqualTo(ApplyFormStatus.ACTIVE);
        assertThat(updated.isRecruiting()).isEqualTo(!recruiting);
        if (recruiting) {
            verifyNoInteractions(fcmService);
        } else {
            var captor = forClass(FCMRequest.class);
            verify(fcmService).sendNotification(captor.capture());
            assertThat(captor.getValue().tokens()).containsExactly("token-B");
            assertThat(captor.getValue().title()).isEqualTo("📢 모집 재개 알림");
            assertThat(captor.getValue().body()).isEqualTo("Club B 동아리의 모집이 시작되었습니다! 지금 바로 확인해보세요.");
        }
        verifyNoInteractions(emailService, s3Service);
    }

    @Test
    @DisplayName("Unauthenticated mutation returns 401 without changing data")
    void mutation_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(patch("/api/admin/clubs/" + clubB.clubId() + "/toggle-recruitment")).andExpect(status().isUnauthorized());
        flushAndClear();
        assertThat(entityManager.find(ApplyForm.class, clubB.formId()).isRecruiting()).isTrue();
        verifyNoInteractions(emailService, fcmService, s3Service);
    }
}
