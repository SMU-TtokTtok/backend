package org.project.ttokttok.domain.temp.applyform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.temp.applyform.domain.TempApplyForm;
import org.project.ttokttok.domain.temp.applyform.repository.TempApplyFormRepository;
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
        "spring.datasource.url=jdbc:h2:mem:tempFormAuthorization;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TempApplyFormAuthorizationIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TempApplyFormRepository repository;
    @Autowired private JwtFactory jwtFactory;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;
    @MockitoBean private S3Service s3Service;

    private Club clubA;
    private Club clubB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        clubA = createClub("A");
        clubB = createClub("B");
        tokenA = token("tempAdminA");
        tokenB = token("tempAdminB");
        flushAndClear();
    }

    private Club createClub(String suffix) {
        Admin admin = Admin.adminJoin("tempAdmin" + suffix, "test-password");
        entityManager.persist(admin);
        Club club = Club.builder().admin(admin).clubName("Temp club " + suffix)
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

    private ResultActions save(String clubId, String title, String token) throws Exception {
        var request = post("/api/admin/temp-applyform").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clubId":"%s","title":"%s","applyStartDate":"2026-11-01",
                        "applyEndDate":"2026-11-30","maxApplyCount":3,"grades":[],"formJson":[]}
                        """.formatted(clubId, title));
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, token);
        }
        ResultActions result = mockMvc.perform(request);
        flushAndClear();
        return result;
    }

    @Test
    void ownClub_createThenUpdate_keepsSameRow() throws Exception {
        save(clubA.getId(), "first", tokenA).andExpect(status().isCreated());
        String id = repository.findByClubId(clubA.getId()).orElseThrow().getId();
        save(clubA.getId(), "updated", tokenA).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tempApplyFormId").value(id));
        assertThat(repository.findByClubId(clubA.getId()).orElseThrow().getTitle()).isEqualTo("updated");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void otherClub_createForbidden_thenOwnerCanSave() throws Exception {
        save(clubB.getId(), "unauthorized", tokenA).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.details").value(ErrorMessage.NOT_CLUB_ADMIN.getMessage()));
        assertThat(repository.findByClubId(clubB.getId())).isEmpty();
        save(clubB.getId(), "owner", tokenB).andExpect(status().isCreated());
        assertThat(repository.findByClubId(clubB.getId()).orElseThrow().getTitle()).isEqualTo("owner");
    }

    @Test
    void otherClub_updateForbidden_preservesExistingRow() throws Exception {
        TempApplyForm original = TempApplyForm.create(clubB.getId(), "original", "subtitle",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), false,
                null, null, 3, Set.of(), List.of());
        repository.save(original);
        flushAndClear();
        save(clubB.getId(), "unauthorized", tokenA).andExpect(status().isForbidden());
        TempApplyForm unchanged = repository.findByClubId(clubB.getId()).orElseThrow();
        assertThat(unchanged.getId()).isEqualTo(original.getId());
        assertThat(unchanged.getTitle()).isEqualTo("original");
        assertThat(unchanged.getSubTitle()).isEqualTo("subtitle");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void missingClub_notFound_withoutWriting() throws Exception {
        save("missing-club", "new", tokenA).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details").value(ErrorMessage.CLUB_NOT_FOUND.getMessage()));
        assertThat(repository.count()).isZero();
    }

    @Test
    void noToken_unauthorized_withoutWriting() throws Exception {
        save(clubA.getId(), "new", null).andExpect(status().isUnauthorized());
        assertThat(repository.count()).isZero();
    }
}
