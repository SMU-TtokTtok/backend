package org.project.ttokttok.domain.applyform.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.repository.dto.ClubRecruitmentQueryDto;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyFormRepositoryTest implements RepositoryTestSupport {

    @Autowired
    private ApplyFormRepository applyFormRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EntityManager em;

    private Club testClub1;
    private Club testClub2;

    @BeforeEach
    void setUp() {
        Admin admin1 = adminRepository.save(
                Admin.adminJoin("applyformadmin1", "password123!"));
        Admin admin2 = adminRepository.save(
                Admin.adminJoin("applyformadmin2", "password123!"));

        testClub1 = clubRepository.save(Club.builder()
                .admin(admin1)
                .clubName("지원폼 테스트 동아리 1")
                .clubUniv(ClubUniv.ENGINEERING)
                .build());

        testClub2 = clubRepository.save(Club.builder()
                .admin(admin2)
                .clubName("지원폼 테스트 동아리 2")
                .clubUniv(ClubUniv.DESIGN)
                .build());

        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        applyFormRepository.deleteAllInBatch();
        clubRepository.deleteAllInBatch();
        adminRepository.deleteAllInBatch();
    }

    private ApplyForm saveApplyForm(Club club, LocalDate applyEndDate) {
        ApplyForm applyForm = ApplyForm.createApplyForm(
                club,
                false,
                LocalDate.now().minusDays(1),
                applyEndDate,
                null,
                null,
                10,
                Set.of(),
                "모집 공고",
                "부제목",
                List.of()
        );
        return applyFormRepository.save(applyForm);
    }

    @Nested
    @DisplayName("findRecruitingFormsByClubIds 메서드")
    class FindRecruitingFormsByClubIdsTest {

        @Test
        @DisplayName("모집중인 지원폼을 동아리 ID와 마감일로 반환한다")
        void findRecruitingFormsByClubIds_Success() {
            LocalDate applyEndDate = LocalDate.now().plusDays(5);
            saveApplyForm(testClub1, applyEndDate);
            em.flush();
            em.clear();

            List<ClubRecruitmentQueryDto> results =
                    applyFormRepository.findRecruitingFormsByClubIds(List.of(testClub1.getId()));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).clubId()).isEqualTo(testClub1.getId());
            assertThat(results.get(0).applyEndDate()).isEqualTo(applyEndDate);
        }

        /**
         * 이 프로젝트의 모집 마감은 {@code status} 를 건드리지 않고 {@code isRecruiting} 만 false 로 바꾼다
         * ({@code ApplyFormScheduler}, {@code ClubAdminService#toggleRecruitment}).
         *
         * <p>즐겨찾기 목록이 {@code status = ACTIVE} 만 보고 모집중으로 표시하던 버그(#393)의 회귀 테스트다.
         */
        @Test
        @DisplayName("status가 ACTIVE여도 isRecruiting이 false면 제외한다")
        void findRecruitingFormsByClubIds_ExcludesNotRecruiting() {
            ApplyForm applyForm = saveApplyForm(testClub1, LocalDate.now().plusDays(5));
            applyForm.endRecruiting();
            em.flush();
            em.clear();

            List<ClubRecruitmentQueryDto> results =
                    applyFormRepository.findRecruitingFormsByClubIds(List.of(testClub1.getId()));

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("status가 INACTIVE면 isRecruiting이 true여도 제외한다")
        void findRecruitingFormsByClubIds_ExcludesInactive() {
            ApplyForm applyForm = saveApplyForm(testClub1, LocalDate.now().plusDays(5));
            applyForm.updateFormStatus(); // ACTIVE -> INACTIVE
            em.flush();
            em.clear();

            List<ClubRecruitmentQueryDto> results =
                    applyFormRepository.findRecruitingFormsByClubIds(List.of(testClub1.getId()));

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("여러 동아리를 한 번의 조회로 처리한다")
        void findRecruitingFormsByClubIds_MultipleClubs() {
            saveApplyForm(testClub1, LocalDate.now().plusDays(5));
            saveApplyForm(testClub2, LocalDate.now().plusDays(10));
            em.flush();
            em.clear();

            List<ClubRecruitmentQueryDto> results = applyFormRepository.findRecruitingFormsByClubIds(
                    List.of(testClub1.getId(), testClub2.getId()));

            assertThat(results)
                    .extracting(ClubRecruitmentQueryDto::clubId)
                    .containsExactlyInAnyOrder(testClub1.getId(), testClub2.getId());
        }

        @Test
        @DisplayName("모집중인 동아리만 결과에 포함된다")
        void findRecruitingFormsByClubIds_OnlyRecruitingClubs() {
            saveApplyForm(testClub1, LocalDate.now().plusDays(5));
            ApplyForm closedForm = saveApplyForm(testClub2, LocalDate.now().plusDays(10));
            closedForm.endRecruiting();
            em.flush();
            em.clear();

            List<ClubRecruitmentQueryDto> results = applyFormRepository.findRecruitingFormsByClubIds(
                    List.of(testClub1.getId(), testClub2.getId()));

            assertThat(results)
                    .extracting(ClubRecruitmentQueryDto::clubId)
                    .containsExactly(testClub1.getId());
        }
    }
}
