package org.project.ttokttok.domain.applicant.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.dto.ApplicantSimpleInfoDto;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.repository.dto.response.ApplicantPageQueryResponse;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApplicantCustomRepositoryImpl} characterization 테스트.
 *
 * <p>서류/면접 분기를 전략 객체로 정리하기에 앞서 <b>현재 동작을 고정</b>하기 위해 작성했다.
 * 따라서 여기의 기대값은 "이렇게 동작해야 한다"가 아니라 "지금 이렇게 동작한다"를 기록한 것이다.
 * 리팩토링 전후로 이 테스트가 수정 없이 통과해야 동작 보존이 증명된다.
 */
class ApplicantCustomRepositoryImplTest implements RepositoryTestSupport {

    private static final String DOCUMENT_KIND = "DOCUMENT";
    private static final String INTERVIEW_KIND = "INTERVIEW";
    private static final String SUBMIT_SORT = "SUBMIT";

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private ApplyFormRepository applyFormRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EntityManager em;

    private ApplyForm form;

    @BeforeEach
    void setUp() {
        Admin admin = adminRepository.save(
                Admin.adminJoin("applicantadmin", "password123!", "applicant-admin@sangmyung.kr"));

        Club club = clubRepository.save(Club.builder()
                .admin(admin)
                .clubName("지원자 조회 테스트 동아리")
                .clubUniv(ClubUniv.ENGINEERING)
                .build());

        form = applyFormRepository.save(ApplyForm.builder()
                .club(club)
                .hasInterview(true)
                .applyStartDate(LocalDate.now().minusDays(7))
                .applyEndDate(LocalDate.now().plusDays(7))
                .maxApplyCount(100)
                .title("모집 공고")
                .subTitle("부제")
                .formJson(List.of())
                .build());
    }

    /** 서류 단계 지원자를 만든다 (EVALUATING 상태). */
    private Applicant givenDocumentApplicant(String name, Grade grade) {
        Applicant applicant = Applicant.createApplicant(
                name + "@sangmyung.kr", name, 22, "컴퓨터공학과",
                name + "@test.com", "010-0000-0000",
                StudentStatus.ENROLLED, grade, Gender.MALE, form);

        applicant.submitDocument(List.of());

        return applicantRepository.save(applicant);
    }

    /** 면접 단계까지 올라간 지원자를 만든다. */
    private Applicant givenInterviewApplicant(String name, Grade grade) {
        Applicant applicant = givenDocumentApplicant(name, grade);
        applicant.passDocumentEvaluation();
        applicant.updateToInterviewPhase(LocalDate.now().plusDays(3));

        return applicantRepository.save(applicant);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private List<String> namesOf(ApplicantPageQueryResponse response) {
        return response.applicants().stream()
                .map(ApplicantSimpleInfoDto::name)
                .toList();
    }

    @Nested
    @DisplayName("findApplicantsPageWithSortCriteria(): 단계별 지원자 페이지 조회")
    class FindApplicantsPage {

        @Test
        @DisplayName("서류 단계 조회는 서류 전형이 있는 지원자만 반환한다")
        void documentKind_returnsDocumentApplicants() {
            givenDocumentApplicant("서류지원자", Grade.SECOND_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.findApplicantsPageWithSortCriteria(
                    null, false, 1, 10, form.getId(), DOCUMENT_KIND);

            assertThat(namesOf(response)).containsExactly("서류지원자");
            assertThat(response.totalCount()).isEqualTo(1);
            assertThat(response.currentPage()).isEqualTo(1);
        }

        @Test
        @DisplayName("서류 단계 조회 결과에는 면접 날짜가 담기지 않는다")
        void documentKind_hasNoInterviewDate() {
            givenDocumentApplicant("서류지원자", Grade.FIRST_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.findApplicantsPageWithSortCriteria(
                    null, false, 1, 10, form.getId(), DOCUMENT_KIND);

            assertThat(response.applicants().get(0).interviewDate()).isNull();
        }

        @Test
        @DisplayName("면접 단계 조회는 면접 전형이 생성된 지원자만 반환하고 면접 날짜를 함께 담는다")
        void interviewKind_returnsInterviewApplicantsWithDate() {
            givenDocumentApplicant("서류만", Grade.FIRST_GRADE);
            givenInterviewApplicant("면접까지", Grade.SECOND_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.findApplicantsPageWithSortCriteria(
                    null, false, 1, 10, form.getId(), INTERVIEW_KIND);

            assertThat(namesOf(response)).containsExactly("면접까지");
            assertThat(response.applicants().get(0).interviewDate()).isEqualTo(LocalDate.now().plusDays(3));
        }

        @Test
        @DisplayName("evaluating=true면 평가중인 지원자만 반환한다")
        void evaluatingFilter_returnsOnlyEvaluating() {
            givenDocumentApplicant("평가중", Grade.FIRST_GRADE);

            Applicant passed = givenDocumentApplicant("합격", Grade.SECOND_GRADE);
            passed.passDocumentEvaluation();
            applicantRepository.save(passed);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.findApplicantsPageWithSortCriteria(
                    null, true, 1, 10, form.getId(), DOCUMENT_KIND);

            assertThat(namesOf(response)).containsExactly("평가중");
        }

        @Test
        @DisplayName("SUBMIT 정렬은 지원 순서(생성일 오름차순)로 반환한다")
        void submitSort_ordersByCreatedAt() {
            givenDocumentApplicant("첫번째", Grade.FOURTH_GRADE);
            givenDocumentApplicant("두번째", Grade.FIRST_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.findApplicantsPageWithSortCriteria(
                    SUBMIT_SORT, false, 1, 10, form.getId(), DOCUMENT_KIND);

            assertThat(namesOf(response)).containsExactly("첫번째", "두번째");
        }

        @Test
        @DisplayName("페이지 크기와 커서에 따라 나눠서 조회한다")
        void pagination_splitsByCursorAndSize() {
            givenDocumentApplicant("A", Grade.FIRST_GRADE);
            givenDocumentApplicant("B", Grade.FIRST_GRADE);
            givenDocumentApplicant("C", Grade.FIRST_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse firstPage = applicantRepository.findApplicantsPageWithSortCriteria(
                    null, false, 1, 2, form.getId(), DOCUMENT_KIND);
            ApplicantPageQueryResponse secondPage = applicantRepository.findApplicantsPageWithSortCriteria(
                    null, false, 2, 2, form.getId(), DOCUMENT_KIND);

            assertThat(firstPage.applicants()).hasSize(2);
            assertThat(firstPage.totalCount()).isEqualTo(3);
            assertThat(firstPage.totalPage()).isEqualTo(2);
            assertThat(secondPage.applicants()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("searchApplicantsByKeyword(): 이름 검색")
    class SearchByKeyword {

        @Test
        @DisplayName("이름에 키워드가 포함된 지원자만 반환한다")
        void filtersByNameKeyword() {
            givenDocumentApplicant("김철수", Grade.FIRST_GRADE);
            givenDocumentApplicant("박영희", Grade.SECOND_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.searchApplicantsByKeyword(
                    "철수", null, false, 1, 10, form.getId(), DOCUMENT_KIND);

            assertThat(namesOf(response)).containsExactly("김철수");
            assertThat(response.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("면접 단계에서도 이름 검색이 동작한다")
        void filtersByNameKeyword_inInterviewPhase() {
            givenInterviewApplicant("김철수", Grade.FIRST_GRADE);
            givenInterviewApplicant("박영희", Grade.SECOND_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.searchApplicantsByKeyword(
                    "영희", null, false, 1, 10, form.getId(), INTERVIEW_KIND);

            assertThat(namesOf(response)).containsExactly("박영희");
        }
    }

    @Nested
    @DisplayName("findApplicantsByStatus(): 합격/불합격 조회")
    class FindByStatus {

        @Test
        @DisplayName("서류 합격자만 반환한다")
        void returnsPassedDocumentApplicants() {
            Applicant passed = givenDocumentApplicant("합격자", Grade.FIRST_GRADE);
            passed.passDocumentEvaluation();
            applicantRepository.save(passed);

            givenDocumentApplicant("평가중", Grade.SECOND_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.findApplicantsByStatus(
                    true, 1, 10, form.getId(), DOCUMENT_KIND);

            assertThat(namesOf(response)).containsExactly("합격자");
        }

        @Test
        @DisplayName("서류 불합격자만 반환한다")
        void returnsFailedDocumentApplicants() {
            Applicant failed = givenDocumentApplicant("불합격자", Grade.FIRST_GRADE);
            failed.failDocumentEvaluation();
            applicantRepository.save(failed);

            givenDocumentApplicant("평가중", Grade.SECOND_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.findApplicantsByStatus(
                    false, 1, 10, form.getId(), DOCUMENT_KIND);

            assertThat(namesOf(response)).containsExactly("불합격자");
        }

        @Test
        @DisplayName("면접 합격자만 반환한다")
        void returnsPassedInterviewApplicants() {
            Applicant passed = givenInterviewApplicant("면접합격", Grade.FIRST_GRADE);
            passed.passInterview();
            applicantRepository.save(passed);

            givenInterviewApplicant("면접평가중", Grade.SECOND_GRADE);
            flushAndClear();

            ApplicantPageQueryResponse response = applicantRepository.findApplicantsByStatus(
                    true, 1, 10, form.getId(), INTERVIEW_KIND);

            assertThat(namesOf(response)).containsExactly("면접합격");
        }
    }

    @Nested
    @DisplayName("getUserApplicationHistory(): 사용자 지원내역")
    class UserApplicationHistory {

        @Test
        @DisplayName("해당 사용자의 지원내역을 최신순으로 반환한다")
        void returnsOwnHistoryInLatestOrder() {
            givenDocumentApplicant("내지원", Grade.FIRST_GRADE);
            flushAndClear();

            var history = applicantRepository.getUserApplicationHistory(
                    "내지원@sangmyung.kr", 10, null, "latest");

            assertThat(history).hasSize(1);
            assertThat(history.get(0).clubName()).isEqualTo("지원자 조회 테스트 동아리");
            assertThat(history.get(0).applyEndDate()).isEqualTo(form.getApplyEndDate());
        }

        @Test
        @DisplayName("다른 사용자의 지원내역은 반환하지 않는다")
        void excludesOtherUsersHistory() {
            givenDocumentApplicant("남의지원", Grade.FIRST_GRADE);
            flushAndClear();

            var history = applicantRepository.getUserApplicationHistory(
                    "관계없는사람@sangmyung.kr", 10, null, "latest");

            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("hasNext 판별을 위해 size+1건까지 조회한다")
        void fetchesOneMoreThanSize() {
            givenDocumentApplicant("지원자", Grade.FIRST_GRADE);
            flushAndClear();

            var history = applicantRepository.getUserApplicationHistory(
                    "지원자@sangmyung.kr", 1, null, "latest");

            assertThat(history).hasSize(1);
        }
    }
}
