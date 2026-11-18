package org.project.ttokttok.domain.club.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.RecruitStatePolicy;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 모집 상태(recruitState) 판정이 조회 경로마다 같은지 검증한다 (#456).
 *
 * <p>QueryDSL 목록, native SQL 인기 목록, 상세, 관리자 상세가 각자 쿼리를 갖고 있어
 * 한 곳만 판정이 어긋나도 화면마다 다른 상태가 보인다. 판정 기준은 {@link RecruitStatePolicy} 다.
 * <ul>
 *   <li>{@code 모집중} — 기간 안, 모집 스위치 켜짐 → OPEN</li>
 *   <li>{@code 모집예정} — 시작일 전, 스위치 켜짐 → UPCOMING</li>
 *   <li>{@code 기간지남} — 마감일 지남, 스위치 켜짐 → CLOSED</li>
 *   <li>{@code 수동마감} — 기간 안, 스위치 꺼짐 → CLOSED</li>
 * </ul>
 *
 * <p>{@code RepositoryTestSupport} 의 PostgreSQLDialect 로는 H2 에 저장한 지원폼 질문(JSON 컬럼)을
 * 다시 읽지 못해 상세 조회가 실패한다. 그래서 H2Dialect 와 별도 인메모리 DB 를 지정한다.
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:clubRecruitState;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@ActiveProfiles("test")
@Transactional
@DisplayName("동아리 카드·상세 조회의 모집 상태 (#456)")
class ClubRecruitStateQueryTest {

    private static final Map<String, RecruitState> EXPECTED_STATES = Map.of(
            "모집중", RecruitState.OPEN,
            "모집예정", RecruitState.UPCOMING,
            "기간지남", RecruitState.CLOSED,
            "수동마감", RecruitState.CLOSED);

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private S3Service s3Service;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private EntityManager em;

    private final Map<String, String> clubIds = new HashMap<>();
    private LocalDate today;
    private int adminSequence;

    @BeforeEach
    void setUp() {
        // given
        today = RecruitStatePolicy.today();
        givenClub("모집중", today.minusDays(1), today.plusDays(5), true);
        givenClub("모집예정", today.plusDays(2), today.plusDays(9), true);
        givenClub("기간지남", today.minusDays(9), today.minusDays(1), true);
        givenClub("수동마감", today.minusDays(1), today.plusDays(5), false);
        em.flush();
        em.clear();
    }

    private void givenClub(String name, LocalDate start, LocalDate end, boolean recruiting) {
        Admin admin = Admin.adminJoin("stateadmin" + adminSequence++, "password123!");
        em.persist(admin);

        Club club = Club.builder()
                .admin(admin)
                .clubName(name)
                .clubUniv(ClubUniv.ENGINEERING)
                .build();
        em.persist(club);
        clubIds.put(name, club.getId());

        ApplyForm form = ApplyForm.createApplyForm(club, false, start, end, null, null, 10,
                Set.of(ApplicableGrade.FIRST_GRADE), name, null,
                List.of(new Question("q1", "지원 동기", null, QuestionType.LONG_ANSWER, false, List.of())));
        if (!recruiting) {
            form.endRecruiting();
        }
        em.persist(form);
    }

    private Map<String, RecruitState> statesByName(List<ClubCardQueryResponse> cards) {
        return cards.stream()
                .collect(Collectors.toMap(ClubCardQueryResponse::name, ClubCardQueryResponse::recruitState));
    }

    @Test
    @DisplayName("목록 조회는 모집 기간과 스위치로 상태를 판정한다")
    void clubList_judgesStateByPeriodAndSwitch() {
        // when
        List<ClubCardQueryResponse> cards = clubRepository.getClubList(
                null, null, null, null, null, 10, null, "latest", null);

        // then
        assertThat(statesByName(cards)).isEqualTo(EXPECTED_STATES);
    }

    @Test
    @DisplayName("모집중 필터는 기간 안이고 스위치가 켜진 동아리만 고른다")
    void recruitingFilter_selectsOnlyOpenClubs() {
        // when
        List<ClubCardQueryResponse> cards = clubRepository.getClubList(
                null, null, null, true, null, 10, null, "latest", null);

        // then
        assertThat(cards).extracting(ClubCardQueryResponse::name).containsExactly("모집중");
    }

    @Test
    @DisplayName("인기 동아리 native 조회도 같은 판정을 쓴다")
    void popularNativeQuery_usesSameJudgement() {
        // when
        List<ClubCardQueryResponse> cards = clubRepository.getAllPopularClubs("nobody@sangmyung.kr", 0.0);

        // then
        assertThat(statesByName(cards)).isEqualTo(EXPECTED_STATES);
    }

    @Test
    @DisplayName("상세와 관리자 상세도 모집 상태를 내린다")
    void detailAndAdminDetail_returnRecruitState() {
        EXPECTED_STATES.forEach((name, state) -> {
            // when
            RecruitState detailState = clubRepository.getClubIntroduction(clubIds.get(name), null).recruitState();
            RecruitState adminState = clubRepository.getAdminClubIntro(clubIds.get(name)).recruitState();

            // then
            assertThat(detailState).as(name).isEqualTo(state);
            assertThat(adminState).as(name).isEqualTo(state);
        });
    }

    @Test
    @DisplayName("관리자 상세는 모집 스위치가 꺼진 활성 폼의 기간과 학년과 인원을 보여준다")
    void adminDetail_showsSwitchedOffActiveForm() {
        // when
        var admin = clubRepository.getAdminClubIntro(clubIds.get("수동마감"));

        // then
        assertThat(admin.applyStartDate()).isEqualTo(today.minusDays(1));
        assertThat(admin.applyDeadLine()).isEqualTo(today.plusDays(5));
        assertThat(admin.grades()).containsExactly(ApplicableGrade.FIRST_GRADE);
        assertThat(admin.maxApplyCount()).isEqualTo(10);
        assertThat(admin.recruitState()).isEqualTo(RecruitState.CLOSED);
    }
}
