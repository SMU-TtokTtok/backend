package org.project.ttokttok.domain.club.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.controller.dto.request.UpdateClubContentRequest;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.jwt.JwtFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClubAdminApiControllerTest {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplyFormRepository applyFormRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtFactory jwtFactory;

    @BeforeEach
    void setUp() {
        String password = passwordEncoder.encode("password1234");
        Admin admin = Admin.adminJoin("testadmin", password);
        adminRepository.save(admin);

        Club club = Club.builder()
                .admin(admin)
                .build();

        clubRepository.save(club);

        // ApplyForm 생성자에 맞는 테스트 데이터로 생성
        ApplyForm applyForm = ApplyForm.builder()
                .club(club)
                .applyStartDate(LocalDateTime.now())
                .applyEndDate(LocalDateTime.now().plusDays(7))
                .maxApplyCount(10)
                .grades(Set.of(ApplicableGrade.FIRST_GRADE, ApplicableGrade.SECOND_GRADE))
                .title("테스트 지원서")
                .subTitle("테스트 부제목")
                .build();

        applyFormRepository.save(applyForm);
    }

    @AfterEach
    void cleanUp() {
        applyFormRepository.deleteAll();
        clubRepository.deleteAll();
        adminRepository.deleteAll();
    }

    @Test
    @DisplayName("updateClubContent(): 동아리 소개 수정에 성공한다.")
    void updateClubContent_success() throws Exception {

        // given
        UpdateClubContentRequest request = new UpdateClubContentRequest(
                JsonNullable.of("동아리"),
                JsonNullable.of(ClubType.CENTRAL),
                JsonNullable.of(ClubCategory.CULTURE),
                JsonNullable.of("기타"),
                JsonNullable.of("소개글"),
                JsonNullable.undefined(),
                JsonNullable.of("본문입니다."),
                JsonNullable.of(true),
                JsonNullable.of(LocalDateTime.from(LocalDateTime.now())),
                JsonNullable.of(LocalDateTime.from(LocalDateTime.now().plusDays(7))),
                JsonNullable.of(Set.of()),
                JsonNullable.of(5)
        );

        MockMultipartFile jsonPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        String validToken = jwtFactory.generateValidToken("testadmin", Role.ROLE_ADMIN);

        String clubId = clubRepository.findAll().get(0).getId(); // 첫 번째 동아리의 ID를 가져옴

        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/clubs/{clubId}/content", clubId)
                        .file(jsonPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .header("Authorization", "Bearer " + validToken) // 필요하면 추가
                )
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Club content updated successfully."));
    }
}
