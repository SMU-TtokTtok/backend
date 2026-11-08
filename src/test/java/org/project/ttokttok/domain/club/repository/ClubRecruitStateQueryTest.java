package org.project.ttokttok.domain.club.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("동아리 카드 조회의 모집 상태 (#456)")
class ClubRecruitStateQueryTest implements RepositoryTestSupport {

    @Autowired private ClubRepository clubRepository;
    @Autowired private EntityManager em;

    private int sequence;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        givenClub("모집중", today.minusDays(1), today.plusDays(5), true);
        givenClub("모집예정", today.plusDays(2), today.plusDays(9), true);
        givenClub("기간지남", today.minusDays(9), today.minusDays(1), true);
        givenClub("수동마감", today.minusDays(1), today.plusDays(5), false);
        em.flush();
        em.clear();
    }

    private void givenClub(String name, LocalDate start, LocalDate end, boolean recruiting) {
        Admin admin = Admin.adminJoin("stateadmin" + sequence++, "password123!");
        em.persist(admin);
        Club club = Club.builder().admin(admin).clubName(name).clubUniv(ClubUniv.ENGINEERING).build();
        em.persist(club);
        ApplyForm form = ApplyForm.createApplyForm(club, false, start, end, null, null, 10,
                Set.of(ApplicableGrade.FIRST_GRADE), name, null,
                List.of(new Question("q1", "지원 동기", null, QuestionType.LONG_ANSWER, false, List.of())));
        if (!recruiting) {
            form.endRecruiting();
        }
        em.persist(form);
    }

    private Map<String, RecruitState> statesByName(List<ClubCardQueryResponse> cards) {
        return cards.stream().collect(Collectors.toMap(ClubCardQueryResponse::name, ClubCardQueryResponse::recruitState));
    }

    private static final Map<String, RecruitState> EXPECTED = Map.of(
            "모집중", RecruitState.OPEN,
            "모집예정", RecruitState.UPCOMING,
            "기간지남", RecruitState.CLOSED,
            "수동마감", RecruitState.CLOSED);

    @Test
    void 목록_조회는_모집_기간과_스위치로_상태를_판정한다() {
        List<ClubCardQueryResponse> cards = clubRepository.getClubList(
                null, null, null, null, null, 10, null, "latest", null);

        assertThat(statesByName(cards)).isEqualTo(EXPECTED);
    }

    @Test
    void 모집중_필터는_기간_안이고_스위치가_켜진_동아리만_고른다() {
        List<ClubCardQueryResponse> cards = clubRepository.getClubList(
                null, null, null, true, null, 10, null, "latest", null);

        assertThat(cards).extracting(ClubCardQueryResponse::name).containsExactly("모집중");
    }

    @Test
    void 인기_동아리_네이티브_조회도_같은_판정을_쓴다() {
        List<ClubCardQueryResponse> cards = clubRepository.getAllPopularClubs("nobody@sangmyung.kr", 0.0);

        assertThat(statesByName(cards)).isEqualTo(EXPECTED);
    }
}
