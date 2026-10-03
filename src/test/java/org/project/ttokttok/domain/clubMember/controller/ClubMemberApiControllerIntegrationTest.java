package org.project.ttokttok.domain.clubMember.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
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
class ClubMemberApiControllerIntegrationTest {
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
        ClubMember member = persist(ClubMember.create(club, "부원 " + suffix, MemberRole.MEMBER,
                Grade.FIRST_GRADE, "컴퓨터공학", "member" + suffix + "@example.com", "01012345678", Gender.MALE));
        return new ClubTestData(club.getId(), member.getId(),
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

    private record ClubTestData(String clubId, String memberId, String adminToken) { }

    @Test
    void changeRole_otherMember_forbidden() throws Exception {
        String clubId = clubA.clubId();

        // when & then
        denied(json(patch(memberUrl(clubId) + "/role"), "{\"role\":\"EXECUTIVE\"}"),
                ErrorMessage.MEMBER_ACCESS_DENIED);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertThat(entityManager.find(ClubMember.class, clubB.memberId()).getRole()).isEqualTo(MemberRole.MEMBER);
    }

    @Test
    void changeRole_otherClub_forbidden() throws Exception {
        String clubId = clubB.clubId();

        // when & then
        denied(json(patch(memberUrl(clubId) + "/role"), "{\"role\":\"EXECUTIVE\"}"),
                ErrorMessage.NOT_CLUB_ADMIN);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertThat(entityManager.find(ClubMember.class, clubB.memberId()).getRole()).isEqualTo(MemberRole.MEMBER);
    }

    @Test
    void changeRole_success() throws Exception {

        // when & then
        perform(json(patch(memberUrl(clubB.clubId()) + "/role"), "{\"role\":\"EXECUTIVE\"}"), clubB)
                .andExpect(status().isOk());
        assertThat(entityManager.find(ClubMember.class, clubB.memberId()).getRole()).isEqualTo(MemberRole.EXECUTIVE);
    }

    @Test
    void deleteMember_otherMember_forbidden() throws Exception {
        String clubId = clubA.clubId();

        // when & then
        denied(delete(memberUrl(clubId)),
                ErrorMessage.MEMBER_ACCESS_DENIED);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertThat(entityManager.find(ClubMember.class, clubB.memberId())).isNotNull();
    }

    @Test
    void deleteMember_otherClub_forbidden() throws Exception {
        String clubId = clubB.clubId();

        // when & then
        denied(delete(memberUrl(clubId)),
                ErrorMessage.NOT_CLUB_ADMIN);
        verifyNoInteractions(emailService, fcmService, s3Service);
        assertThat(entityManager.find(ClubMember.class, clubB.memberId())).isNotNull();
    }

    @Test
    void deleteMember_success() throws Exception {

        // when & then
        perform(delete(memberUrl(clubB.clubId())), clubB).andExpect(status().isOk());
        assertThat(entityManager.find(ClubMember.class, clubB.memberId())).isNull();
    }

    private String memberUrl(String clubId) {
        return "/api/admin/members/" + clubId + "/" + clubB.memberId();
    }

    @Test
    @DisplayName("Unauthenticated mutation returns 401 without changing data")
    void mutation_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(json(patch(memberUrl(clubB.clubId()) + "/role"), "{\"role\":\"EXECUTIVE\"}")).andExpect(status().isUnauthorized());
        flushAndClear();
        assertThat(entityManager.find(ClubMember.class, clubB.memberId()).getRole()).isEqualTo(MemberRole.MEMBER);
        verifyNoInteractions(emailService, fcmService, s3Service);
    }
}
