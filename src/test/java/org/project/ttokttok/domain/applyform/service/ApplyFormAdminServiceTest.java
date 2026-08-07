package org.project.ttokttok.domain.applyform.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.AlreadyActiveApplyFormExistsException;
import org.project.ttokttok.domain.applyform.exception.InvalidDateRangeException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormCreateServiceRequest;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.temp.applyform.domain.TempApplyForm;
import org.project.ttokttok.domain.temp.applyform.repository.TempApplyFormRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link ApplyFormAdminService#createApplyForm} 단위 테스트.
 *
 * <p>이 서비스는 지원 폼 생성 시 <b>동아리 존재 → 관리자 일치 → 날짜 범위 → 활성 폼 중복</b> 순으로만
 * 검증한다. 나머지 입력값(학년, 질문 목록, 모집 인원, 면접 일정)은 서비스에서 검증하지 않고
 * 그대로 엔티티에 전달되므로, 어떤 비정형 입력이 어디까지 통과하는지를 여기서 명시적으로 고정한다.
 *
 * <p>"검증 없이 통과한다"는 단언은 <b>바람직한 동작이라는 뜻이 아니라 현재 동작의 기록</b>이다.
 * 검증을 추가하는 순간 해당 테스트가 실패하므로, 방어 로직 추가 시 이 파일이 변경 지점을 알려준다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplyFormAdminService - 지원 폼 생성")
class ApplyFormAdminServiceTest {

    private static final String CLUB_ID = "club-1";
    private static final String ADMIN_USERNAME = "clubadmin";
    /** {@code Admin} 은 관리자 명이 8자 이상이어야 하므로 타인 계정도 길이를 맞춘다. */
    private static final String OTHER_ADMIN_USERNAME = "otheradmin";
    private static final String SAVED_FORM_ID = "form-1";

    private static final LocalDate RECRUIT_START = LocalDate.of(2026, 3, 1);
    private static final LocalDate RECRUIT_END = LocalDate.of(2026, 3, 10);
    private static final LocalDate INTERVIEW_START = LocalDate.of(2026, 3, 15);
    private static final LocalDate INTERVIEW_END = LocalDate.of(2026, 3, 20);

    @Mock
    private ApplyFormRepository applyFormRepository;

    @Mock
    private ClubRepository clubRepository;

    /**
     * 기존 테스트에는 이 목이 없어 {@code tempApplyFormRepository} 가 null 로 주입됐다.
     * 생성 성공 경로는 반드시 이 리포지토리를 거치므로 NPE 로 막혀 있었고,
     * 그래서 성공 케이스 테스트가 빈 채로 주석 처리돼 있었다.
     */
    @Mock
    private TempApplyFormRepository tempApplyFormRepository;

    @InjectMocks
    private ApplyFormAdminService applyFormAdminService;

    @Captor
    private ArgumentCaptor<ApplyForm> applyFormCaptor;

    private static Club givenClubOwnedBy(String adminUsername) {
        Admin admin = Admin.adminJoin(adminUsername, "password123!", "admin@sangmyung.kr");

        return Club.builder()
                .admin(admin)
                .clubName("똑똑동아리")
                .clubUniv(ClubUniv.ENGINEERING)
                .build();
    }

    private static Question givenQuestion(String questionId) {
        return new Question(questionId, "지원 동기", "자유롭게 작성해주세요",
                QuestionType.LONG_ANSWER, true, List.of());
    }

    /** 기본값이 모두 유효한 요청. 각 테스트는 검증하려는 필드만 바꿔서 쓴다. */
    private static ApplyFormCreateServiceRequest.ApplyFormCreateServiceRequestBuilder validRequest() {
        return ApplyFormCreateServiceRequest.builder()
                .username(ADMIN_USERNAME)
                .clubId(CLUB_ID)
                .hasInterview(true)
                .recruitStartDate(RECRUIT_START)
                .recruitEndDate(RECRUIT_END)
                .interviewStartDate(INTERVIEW_START)
                .interviewEndDate(INTERVIEW_END)
                .applicableGrades(Set.of(1, 2))
                .maxApplyCount(30)
                .title("2026-1 신입 부원 모집")
                .subTitle("많은 지원 바랍니다")
                .questions(List.of(givenQuestion("q1")));
    }

    /** 동아리 조회까지만 통과시킨다. 관리자 일치 이후 단계를 보지 않는 테스트용. */
    private void givenExistingClub() {
        given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(givenClubOwnedBy(ADMIN_USERNAME)));
    }

    /** 활성 폼 중복 검사까지 통과시켜 실제 저장이 일어나도록 한다. */
    private void givenCreatableClub() {
        givenExistingClub();
        given(applyFormRepository.existsByClubIdAndStatus(eq(CLUB_ID), eq(ApplyFormStatus.ACTIVE)))
                .willReturn(false);
        given(tempApplyFormRepository.findByClubId(any())).willReturn(Optional.empty());
        given(applyFormRepository.save(any(ApplyForm.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    /** {@code save()} 에 전달된 엔티티를 꺼낸다. */
    private ApplyForm savedApplyForm() {
        verify(applyFormRepository).save(applyFormCaptor.capture());

        return applyFormCaptor.getValue();
    }

    @Nested
    @DisplayName("생성 성공")
    class CreateSuccess {

        @Test
        @DisplayName("유효한 요청이면 저장된 폼의 ID를 반환한다")
        void returnsSavedFormId() {
            // given
            givenExistingClub();
            given(applyFormRepository.existsByClubIdAndStatus(any(), any())).willReturn(false);
            given(tempApplyFormRepository.findByClubId(any())).willReturn(Optional.empty());

            ApplyForm saved = mock(ApplyForm.class);
            given(saved.getId()).willReturn(SAVED_FORM_ID);
            given(applyFormRepository.save(any(ApplyForm.class))).willReturn(saved);

            // when
            String formId = applyFormAdminService.createApplyForm(validRequest().build());

            // then
            assertThat(formId).isEqualTo(SAVED_FORM_ID);
        }

        @Test
        @DisplayName("요청 값이 그대로 담긴 엔티티를 저장한다")
        void savesEntityWithRequestValues() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest().build());

            // then
            ApplyForm saved = savedApplyForm();
            assertThat(saved.getTitle()).isEqualTo("2026-1 신입 부원 모집");
            assertThat(saved.getSubTitle()).isEqualTo("많은 지원 바랍니다");
            assertThat(saved.getApplyStartDate()).isEqualTo(RECRUIT_START);
            assertThat(saved.getApplyEndDate()).isEqualTo(RECRUIT_END);
            assertThat(saved.getInterviewStartDate()).isEqualTo(INTERVIEW_START);
            assertThat(saved.getInterviewEndDate()).isEqualTo(INTERVIEW_END);
            assertThat(saved.getMaxApplyCount()).isEqualTo(30);
            assertThat(saved.isHasInterview()).isTrue();
            assertThat(saved.getFormJson()).hasSize(1);
        }

        @Test
        @DisplayName("생성된 폼은 ACTIVE 상태이며 모집중으로 시작한다")
        void startsAsActiveAndRecruiting() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest().build());

            // then
            ApplyForm saved = savedApplyForm();
            assertThat(saved.getStatus()).isEqualTo(ApplyFormStatus.ACTIVE);
            assertThat(saved.isRecruiting()).isTrue();
        }

        @Test
        @DisplayName("학년 숫자를 ApplicableGrade로 변환해 저장한다")
        void convertsGradeNumbersToEnum() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(
                    validRequest().applicableGrades(Set.of(1, 2, 3, 4)).build());

            // then
            assertThat(savedApplyForm().getGrades()).containsExactlyInAnyOrder(
                    ApplicableGrade.FIRST_GRADE, ApplicableGrade.SECOND_GRADE,
                    ApplicableGrade.THIRD_GRADE, ApplicableGrade.FOURTH_GRADE);
        }

        @Test
        @DisplayName("모집 시작일과 마감일이 같은 날이어도 생성된다 - 경계값")
        void allowsSameStartAndEndDate() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest()
                    .recruitStartDate(RECRUIT_START)
                    .recruitEndDate(RECRUIT_START)
                    .build());

            // then
            assertThat(savedApplyForm().getApplyEndDate()).isEqualTo(RECRUIT_START);
        }

        @Test
        @DisplayName("임시 저장된 폼이 있으면 삭제한 뒤 생성한다")
        void deletesTempFormWhenPresent() {
            // given
            givenExistingClub();
            given(applyFormRepository.existsByClubIdAndStatus(any(), any())).willReturn(false);
            given(applyFormRepository.save(any(ApplyForm.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            TempApplyForm tempForm = mock(TempApplyForm.class);
            given(tempApplyFormRepository.findByClubId(any())).willReturn(Optional.of(tempForm));

            // when
            applyFormAdminService.createApplyForm(validRequest().build());

            // then
            verify(tempApplyFormRepository).delete(tempForm);
        }

        @Test
        @DisplayName("임시 저장된 폼이 없으면 삭제를 시도하지 않는다")
        void doesNotDeleteWhenNoTempForm() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest().build());

            // then
            verify(tempApplyFormRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("비정형 데이터 - 예외를 발생시키는 입력")
    class InvalidInputRejected {

        @ParameterizedTest(name = "학년 {0}은 허용되지 않는다")
        @ValueSource(ints = {0, 5, -1, 100})
        @DisplayName("1~4 범위를 벗어난 학년이 섞이면 예외가 발생한다")
        void rejectsGradeOutOfRange(int invalidGrade) {
            // given
            givenExistingClub();
            given(applyFormRepository.existsByClubIdAndStatus(any(), any())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(
                    validRequest().applicableGrades(Set.of(1, invalidGrade)).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("잘못된 학년입니다");

            verify(applyFormRepository, never()).save(any());
        }

        @Test
        @DisplayName("학년 집합이 null이면 NPE가 발생한다 - 서비스가 방어하지 않음")
        void nullGradeSetThrowsNpe() {
            // given
            givenExistingClub();
            given(applyFormRepository.existsByClubIdAndStatus(any(), any())).willReturn(false);

            // when & then
            // 컨트롤러의 Bean Validation 에 의존하고 있어 서비스 단독으로는 NPE 로 드러난다.
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(
                    validRequest().applicableGrades(null).build()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("모집 시작일이 마감일보다 늦으면 예외가 발생한다")
        void rejectsReversedRecruitDates() {
            // given
            givenExistingClub();

            // when & then
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(validRequest()
                    .recruitStartDate(RECRUIT_END)
                    .recruitEndDate(RECRUIT_START)
                    .build()))
                    .isInstanceOf(InvalidDateRangeException.class);

            verify(applyFormRepository, never()).save(any());
        }

        @Test
        @DisplayName("모집 시작일이 null이면 NPE가 발생한다 - 서비스가 방어하지 않음")
        void nullRecruitStartDateThrowsNpe() {
            // given
            givenExistingClub();

            // when & then
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(
                    validRequest().recruitStartDate(null).build()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("이미 활성 지원 폼이 있으면 예외가 발생한다")
        void rejectsWhenActiveFormExists() {
            // given
            givenExistingClub();
            given(applyFormRepository.existsByClubIdAndStatus(eq(CLUB_ID), eq(ApplyFormStatus.ACTIVE)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(validRequest().build()))
                    .isInstanceOf(AlreadyActiveApplyFormExistsException.class);

            verify(applyFormRepository, never()).save(any());
        }

        @Test
        @DisplayName("동아리가 존재하지 않으면 예외가 발생한다")
        void rejectsWhenClubNotFound() {
            // given
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(validRequest().build()))
                    .isInstanceOf(ClubNotFoundException.class);
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 예외가 발생한다")
        void rejectsWhenNotClubAdmin() {
            // given
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(givenClubOwnedBy(OTHER_ADMIN_USERNAME)));

            // when & then
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(validRequest().build()))
                    .isInstanceOf(NotClubAdminException.class);

            verify(applyFormRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("비정형 데이터 - 검증 없이 통과하는 입력 (현재 동작 기록)")
    class InvalidInputAccepted {

        @Test
        @DisplayName("지원 가능 학년이 비어 있어도 생성된다 - 아무도 지원할 수 없는 폼이 만들어진다")
        void acceptsEmptyGrades() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest().applicableGrades(Set.of()).build());

            // then
            assertThat(savedApplyForm().getGrades()).isEmpty();
        }

        @Test
        @DisplayName("질문 목록이 비어 있어도 생성된다")
        void acceptsEmptyQuestions() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest().questions(List.of()).build());

            // then
            assertThat(savedApplyForm().getFormJson()).isEmpty();
        }

        @Test
        @DisplayName("질문 목록이 null이면 null인 채로 저장 요청된다 - DB의 not null 제약에서만 걸린다")
        void acceptsNullQuestions() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest().questions(null).build());

            // then
            // formJson 은 nullable = false 라 실제 저장 시점에야 실패한다.
            assertThat(savedApplyForm().getFormJson()).isNull();
        }

        @ParameterizedTest(name = "모집 인원 {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("모집 인원이 0 이하여도 생성된다")
        void acceptsNonPositiveMaxApplyCount(int maxApplyCount) {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(
                    validRequest().maxApplyCount(maxApplyCount).build());

            // then
            assertThat(savedApplyForm().getMaxApplyCount()).isEqualTo(maxApplyCount);
        }

        @Test
        @DisplayName("면접 전형이 있다고 했는데 면접 일정이 없어도 생성된다")
        void acceptsMissingInterviewDatesWhenInterviewEnabled() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest()
                    .hasInterview(true)
                    .interviewStartDate(null)
                    .interviewEndDate(null)
                    .build());

            // then
            ApplyForm saved = savedApplyForm();
            assertThat(saved.isHasInterview()).isTrue();
            assertThat(saved.getInterviewStartDate()).isNull();
            assertThat(saved.getInterviewEndDate()).isNull();
        }

        @Test
        @DisplayName("면접 시작일이 종료일보다 늦어도 검증하지 않는다 - 모집 일정에만 검증이 걸려 있다")
        void acceptsReversedInterviewDates() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest()
                    .interviewStartDate(INTERVIEW_END)
                    .interviewEndDate(INTERVIEW_START)
                    .build());

            // then
            ApplyForm saved = savedApplyForm();
            assertThat(saved.getInterviewStartDate()).isEqualTo(INTERVIEW_END);
            assertThat(saved.getInterviewEndDate()).isEqualTo(INTERVIEW_START);
        }

        @Test
        @DisplayName("면접 일정이 모집 기간보다 앞서도 검증하지 않는다")
        void acceptsInterviewBeforeRecruitPeriod() {
            // given
            givenCreatableClub();
            LocalDate beforeRecruit = RECRUIT_START.minusDays(10);

            // when
            applyFormAdminService.createApplyForm(validRequest()
                    .interviewStartDate(beforeRecruit)
                    .interviewEndDate(beforeRecruit.plusDays(1))
                    .build());

            // then
            assertThat(savedApplyForm().getInterviewStartDate()).isEqualTo(beforeRecruit);
        }

        @Test
        @DisplayName("제목이 공백만 있어도 생성된다")
        void acceptsBlankTitle() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest().title("   ").build());

            // then
            assertThat(savedApplyForm().getTitle()).isEqualTo("   ");
        }

        @Test
        @DisplayName("부제목이 null이어도 생성된다")
        void acceptsNullSubTitle() {
            // given
            givenCreatableClub();

            // when
            applyFormAdminService.createApplyForm(validRequest().subTitle(null).build());

            // then
            assertThat(savedApplyForm().getSubTitle()).isNull();
        }
    }

    @Nested
    @DisplayName("검증 순서")
    class ValidationOrder {

        @Test
        @DisplayName("관리자 검증이 날짜 검증보다 먼저 수행된다")
        void adminIsValidatedBeforeDates() {
            // given
            // 관리자 불일치와 날짜 역전이 동시에 있는 요청
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(givenClubOwnedBy(OTHER_ADMIN_USERNAME)));

            // when & then
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(validRequest()
                    .recruitStartDate(RECRUIT_END)
                    .recruitEndDate(RECRUIT_START)
                    .build()))
                    .isInstanceOf(NotClubAdminException.class);
        }

        @Test
        @DisplayName("날짜 검증이 활성 폼 중복 검사보다 먼저 수행된다")
        void datesAreValidatedBeforeActiveFormCheck() {
            // given
            givenExistingClub();

            // when & then
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(validRequest()
                    .recruitStartDate(RECRUIT_END)
                    .recruitEndDate(RECRUIT_START)
                    .build()))
                    .isInstanceOf(InvalidDateRangeException.class);

            // 활성 폼 조회 자체가 일어나지 않아야 순서가 보장된다.
            verify(applyFormRepository, never()).existsByClubIdAndStatus(any(), any());
        }

        @Test
        @DisplayName("활성 폼 중복 검사가 임시 폼 삭제보다 먼저 수행된다 - 중복 시 임시 폼이 보존된다")
        void activeFormCheckRunsBeforeTempFormDeletion() {
            // given
            givenExistingClub();
            given(applyFormRepository.existsByClubIdAndStatus(any(), any())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> applyFormAdminService.createApplyForm(validRequest().build()))
                    .isInstanceOf(AlreadyActiveApplyFormExistsException.class);

            verify(tempApplyFormRepository, never()).delete(any());
        }
    }
}
