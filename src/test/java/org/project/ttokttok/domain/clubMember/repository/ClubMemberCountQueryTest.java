package org.project.ttokttok.domain.clubMember.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberCountQueryDto;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ClubMemberCountQueryTest implements RepositoryTestSupport {

    @Autowired
    private ClubMemberRepository clubMemberRepository;

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
                Admin.adminJoin("membercountadmin1", "password123!", "membercount1@sangmyung.kr"));
        Admin admin2 = adminRepository.save(
                Admin.adminJoin("membercountadmin2", "password123!", "membercount2@sangmyung.kr"));

        testClub1 = clubRepository.save(Club.builder()
                .admin(admin1)
                .clubName("멤버수 테스트 동아리 1")
                .clubUniv(ClubUniv.ENGINEERING)
                .build());

        testClub2 = clubRepository.save(Club.builder()
                .admin(admin2)
                .clubName("멤버수 테스트 동아리 2")
                .clubUniv(ClubUniv.DESIGN)
                .build());

        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        clubMemberRepository.deleteAllInBatch();
        clubRepository.deleteAllInBatch();
        adminRepository.deleteAllInBatch();
    }

    private void saveMember(Club club, String name, String email) {
        clubMemberRepository.save(ClubMember.create(
                club, name, MemberRole.MEMBER, Grade.FIRST_GRADE, "컴퓨터과학전공",
                email, "010-0000-0000", Gender.MALE));
    }

    @Test
    @DisplayName("동아리별 멤버 수를 한 번의 조회로 집계한다")
    void countClubMembersForEach_Success() {
        saveMember(testClub1, "멤버1", "member1@sangmyung.kr");
        saveMember(testClub1, "멤버2", "member2@sangmyung.kr");
        saveMember(testClub2, "멤버3", "member3@sangmyung.kr");
        em.flush();
        em.clear();

        List<ClubMemberCountQueryDto> results = clubMemberRepository.countClubMembersForEach(
                List.of(testClub1.getId(), testClub2.getId()));

        assertThat(results)
                .extracting(ClubMemberCountQueryDto::clubId, ClubMemberCountQueryDto::count)
                .containsExactlyInAnyOrder(
                        tuple(testClub1.getId(), 2L),
                        tuple(testClub2.getId(), 1L));
    }

    /**
     * 멤버가 없는 동아리는 GROUP BY 결과에서 빠진다.
     * 호출부({@code FavoriteService})가 0 으로 기본값 처리하는 이유다.
     */
    @Test
    @DisplayName("멤버가 없는 동아리는 결과에 포함되지 않는다")
    void countClubMembersForEach_ExcludesClubsWithoutMembers() {
        saveMember(testClub1, "멤버1", "member1@sangmyung.kr");
        em.flush();
        em.clear();

        List<ClubMemberCountQueryDto> results = clubMemberRepository.countClubMembersForEach(
                List.of(testClub1.getId(), testClub2.getId()));

        assertThat(results)
                .extracting(ClubMemberCountQueryDto::clubId)
                .containsExactly(testClub1.getId());
    }
}
