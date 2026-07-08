package org.project.ttokttok.domain.clubMember.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberCountQueryResponse;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberInExcelResponse;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClubMemberCustomRepositoryImplTest implements RepositoryTestSupport {

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EntityManager em;

    private Club club1;
    private Club club2;

    private ClubMember president; // club1, PRESIDENT, FIRST_GRADE
    private ClubMember executive; // club1, EXECUTIVE, SECOND_GRADE
    private ClubMember member1;   // club1, MEMBER, THIRD_GRADE
    private ClubMember member2;   // club1, MEMBER, FIRST_GRADE
    private ClubMember otherClubMember; // club2, MEMBER

    @BeforeEach
    void setUp() {
        Admin admin1 = adminRepository.save(Admin.adminJoin("testadmin1", "password123!", "admin1@sangmyung.kr"));
        Admin admin2 = adminRepository.save(Admin.adminJoin("testadmin2", "password123!", "admin2@sangmyung.kr"));

        club1 = clubRepository.save(Club.builder()
                .admin(admin1)
                .clubName("테스트 동아리 1")
                .clubUniv(ClubUniv.ENGINEERING)
                .build());

        club2 = clubRepository.save(Club.builder()
                .admin(admin2)
                .clubName("테스트 동아리 2")
                .clubUniv(ClubUniv.DESIGN)
                .build());

        president = clubMemberRepository.save(ClubMember.create(
                club1, "박대표", MemberRole.PRESIDENT, Grade.FIRST_GRADE,
                "경영학과", "president@sangmyung.kr", "010-1111-0001", Gender.MALE));

        executive = clubMemberRepository.save(ClubMember.create(
                club1, "이임원", MemberRole.EXECUTIVE, Grade.SECOND_GRADE,
                "전자공학과", "executive@sangmyung.kr", "010-1111-0002", Gender.FEMALE));

        member1 = clubMemberRepository.save(ClubMember.create(
                club1, "김철수", MemberRole.MEMBER, Grade.THIRD_GRADE,
                "컴퓨터공학과", "member1@sangmyung.kr", "010-1111-0003", Gender.MALE));

        member2 = clubMemberRepository.save(ClubMember.create(
                club1, "최부원", MemberRole.MEMBER, Grade.FIRST_GRADE,
                "화학공학과", "member2@sangmyung.kr", "010-1111-0004", Gender.FEMALE));

        otherClubMember = clubMemberRepository.save(ClubMember.create(
                club2, "정회원", MemberRole.MEMBER, Grade.FIRST_GRADE,
                "디자인학과", "other@sangmyung.kr", "010-1111-0005", Gender.MALE));

        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        clubMemberRepository.deleteAllInBatch();
        clubRepository.deleteAllInBatch();
        adminRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("findClubMemberPageByClubId 메서드")
    class FindClubMemberPageByClubIdTest {

        @Test
        @DisplayName("역할(PRESIDENT>EXECUTIVE>MEMBER) 순, 학년 순으로 정렬된 첫 페이지를 반환한다")
        void findClubMemberPageByClubId_firstPage() {
            // when
            ClubMemberPageQueryResponse result =
                    clubMemberRepository.findClubMemberPageByClubId(club1.getId(), 1, 2);

            // then
            assertThat(result.currentPage()).isEqualTo(1);
            assertThat(result.totalCount()).isEqualTo(4);
            assertThat(result.totalPage()).isEqualTo(2);
            assertThat(result.clubMembers()).hasSize(2);
            assertThat(result.clubMembers().get(0).getId()).isEqualTo(president.getId());
            assertThat(result.clubMembers().get(1).getId()).isEqualTo(executive.getId());
        }

        @Test
        @DisplayName("두 번째 페이지는 학년 오름차순으로 MEMBER들을 반환한다")
        void findClubMemberPageByClubId_secondPage() {
            // when
            ClubMemberPageQueryResponse result =
                    clubMemberRepository.findClubMemberPageByClubId(club1.getId(), 2, 2);

            // then
            assertThat(result.currentPage()).isEqualTo(2);
            assertThat(result.clubMembers()).hasSize(2);
            assertThat(result.clubMembers().get(0).getId()).isEqualTo(member2.getId()); // FIRST_GRADE
            assertThat(result.clubMembers().get(1).getId()).isEqualTo(member1.getId()); // THIRD_GRADE
        }

        @Test
        @DisplayName("다른 동아리의 부원은 결과에 포함되지 않는다")
        void findClubMemberPageByClubId_excludesOtherClub() {
            // when
            ClubMemberPageQueryResponse result =
                    clubMemberRepository.findClubMemberPageByClubId(club2.getId(), 1, 10);

            // then
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.clubMembers()).hasSize(1);
            assertThat(result.clubMembers().get(0).getId()).isEqualTo(otherClubMember.getId());
        }
    }

    @Nested
    @DisplayName("countClubMembersByClubId 메서드")
    class CountClubMembersByClubIdTest {

        @Test
        @DisplayName("동아리의 학년별 부원 수와 총 부원 수를 반환한다")
        void countClubMembersByClubId_success() {
            // when
            ClubMemberCountQueryResponse result = clubMemberRepository.countClubMembersByClubId(club1.getId());

            // then
            assertThat(result.totalCount()).isEqualTo(4);
            assertThat(result.firstGradeCount()).isEqualTo(2); // president, member2
            assertThat(result.secondGradeCount()).isEqualTo(1); // executive
            assertThat(result.thirdGradeCount()).isEqualTo(1); // member1
            assertThat(result.fourthGradeCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findByClubIdAndKeyword 메서드")
    class FindByClubIdAndKeywordTest {

        @Test
        @DisplayName("이름에 키워드가 포함된 부원을 조회한다")
        void findByClubIdAndKeyword_found() {
            // when
            List<ClubMember> result = clubMemberRepository.findByClubIdAndKeyword(club1.getId(), "철수");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(member1.getId());
        }

        @Test
        @DisplayName("일치하는 이름이 없으면 빈 리스트를 반환한다")
        void findByClubIdAndKeyword_notFound() {
            // when
            List<ClubMember> result = clubMemberRepository.findByClubIdAndKeyword(club1.getId(), "존재하지않음");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByClubId 메서드")
    class FindByClubIdTest {

        @Test
        @DisplayName("동아리의 모든 부원을 엑셀 응답 형태로 조회한다")
        void findByClubId_success() {
            // when
            List<ClubMemberInExcelResponse> result = clubMemberRepository.findByClubId(club1.getId());

            // then
            assertThat(result).hasSize(4);
            assertThat(result)
                    .anySatisfy(response -> {
                        assertThat(response.name()).isEqualTo("김철수");
                        assertThat(response.grade()).isEqualTo(Grade.THIRD_GRADE);
                        assertThat(response.major()).isEqualTo("컴퓨터공학과");
                        assertThat(response.role()).isEqualTo(MemberRole.MEMBER);
                    });
        }
    }

    @Nested
    @DisplayName("findByClubIdAndRole 메서드")
    class FindByClubIdAndRoleTest {

        @Test
        @DisplayName("동아리의 회장을 조회한다")
        void findByClubIdAndRole_found() {
            // when
            Optional<ClubMember> result = clubMemberRepository.findByClubIdAndRole(club1.getId(), MemberRole.PRESIDENT);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(president.getId());
        }

        @Test
        @DisplayName("동아리에 부회장이 없으면 빈 값을 반환한다")
        void findByClubIdAndRole_notFound() {
            // when
            Optional<ClubMember> result =
                    clubMemberRepository.findByClubIdAndRole(club1.getId(), MemberRole.VICE_PRESIDENT);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByClubIdAndEmail 메서드")
    class ExistsByClubIdAndEmailTest {

        @Test
        @DisplayName("동아리에 해당 이메일의 부원이 존재하면 true를 반환한다")
        void existsByClubIdAndEmail_true() {
            // when
            boolean exists = clubMemberRepository.existsByClubIdAndEmail(club1.getId(), "member1@sangmyung.kr");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("동아리에 해당 이메일의 부원이 없으면 false를 반환한다")
        void existsByClubIdAndEmail_false() {
            // when
            boolean exists = clubMemberRepository.existsByClubIdAndEmail(club1.getId(), "notexist@sangmyung.kr");

            // then
            assertThat(exists).isFalse();
        }
    }
}
