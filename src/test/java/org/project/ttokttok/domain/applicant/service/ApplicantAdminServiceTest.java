package org.project.ttokttok.domain.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.controller.dto.request.MailFormatRequest;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.DocumentPhase;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.exception.ApplicantNotFoundException;
import org.project.ttokttok.domain.applicant.exception.UnAuthorizedApplicantAccessException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.repository.dto.response.ApplicantPageQueryResponse;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantFinalizationRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantPageServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantSearchServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantStatusServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.SendResultMailServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.StatusUpdateServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantFinalizeServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantPageServiceResponse;
import org.project.ttokttok.domain.club.service.policy.ClubAccessPolicy;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.exception.ActiveApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApplicantAdminServiceTest {

    @InjectMocks
    private ApplicantAdminService applicantAdminService;

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private ApplyFormRepository applyFormRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private EmailService emailService;

    @Spy
    private ClubAccessPolicy clubAccessPolicy;

    private static final String USERNAME = "adminUser";
    private static final String CLUB_ID = "club-1";
    private static final String APPLY_FORM_ID = "form-1";
    private static final String APPLICANT_ID = "applicant-1";
    private static final String OTHER_USERNAME = "otherAdmin";
    private static final String OTHER_CLUB_ID = "other-club";

    private Club createClub(String clubId) {
        return createClub(clubId, USERNAME);
    }

    private Club createClub(String clubId, String adminUsername) {
        Admin admin = mock(Admin.class);
        // username 조회 여부는 서비스의 권한 확인 방식에 따라 달라진다.
        lenient().when(admin.getUsername()).thenReturn(adminUsername);
        Club club = Club.builder().admin(admin).clubName("테스트 동아리").build();
        ReflectionTestUtils.setField(club, "id", clubId);
        return club;
    }

    private SendResultMailServiceRequest createResultMailRequest() {
        return SendResultMailServiceRequest.builder()
                .pass(new MailFormatRequest("합격 안내", "축하합니다."))
                .fail(new MailFormatRequest("불합격 안내", "아쉽습니다."))
                .build();
    }

    @Nested
    @DisplayName("관리자 권한 및 요청 동아리 소속 검증")
    class AccessValidationTest {

        @Test
        @DisplayName("비관리자의 상세 조회는 지원자를 조회하기 전에 거부한다")
        void getApplicantDetail_notAdmin_forbidden() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicantAdminService.getApplicantDetail(USERNAME, APPLICANT_ID))
                    .isExactlyInstanceOf(NotClubAdminException.class);
            verifyNoInteractions(applicantRepository);
        }

        @Test
        @DisplayName("비관리자의 상태 변경은 지원자를 조회하기 전에 거부한다")
        void updateApplicantStatus_notAdmin_forbidden() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());
            StatusUpdateServiceRequest request = StatusUpdateServiceRequest.of(
                    USERNAME, APPLICANT_ID, PhaseStatus.PASS, "DOCUMENT");

            // when & then
            assertThatThrownBy(() -> applicantAdminService.updateApplicantStatus(request))
                    .isExactlyInstanceOf(NotClubAdminException.class);
            verifyNoInteractions(applicantRepository, clubMemberRepository, emailService);
        }

        @Test
        @DisplayName("비관리자의 합불 상태별 조회는 지원폼과 지원자를 조회하지 않는다")
        void getApplicantsByStatus_notAdmin_forbidden() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());
            ApplicantStatusServiceRequest request = ApplicantStatusServiceRequest.of(
                    USERNAME, true, 1, 4, "DOCUMENT");

            // when & then
            assertThatThrownBy(() -> applicantAdminService.getApplicantsByStatus(request))
                    .isExactlyInstanceOf(NotClubAdminException.class);
            verifyNoInteractions(applyFormRepository, applicantRepository);
        }

        @Test
        @DisplayName("다른 동아리의 전형 마감은 거부하고 지원자 처리 및 부원 등록을 하지 않는다")
        void finalizeApplicantsStatus_otherClub_forbidden() {
            // given
            Club otherClub = createClub(OTHER_CLUB_ID, OTHER_USERNAME);
            given(clubRepository.findById(OTHER_CLUB_ID))
                    .willReturn(Optional.of(otherClub));

            ApplicantFinalizationRequest request = ApplicantFinalizationRequest.of(
                    USERNAME, OTHER_CLUB_ID, "DOCUMENT");

            // when & then
            assertThatThrownBy(() -> applicantAdminService.finalizeApplicantsStatus(request))
                    .isExactlyInstanceOf(NotClubAdminException.class);
            verifyNoInteractions(applyFormRepository, applicantRepository, clubMemberRepository, emailService);
        }

        @Test
        @DisplayName("다른 동아리의 결과 메일 발송은 거부하고 지원자 조회 및 발송을 하지 않는다")
        void sendResultMailToApplicants_otherClub_forbidden() {
            // given
            Club otherClub = createClub(OTHER_CLUB_ID, OTHER_USERNAME);

            given(clubRepository.findById(OTHER_CLUB_ID))
                    .willReturn(Optional.of(otherClub));
            SendResultMailServiceRequest request = createResultMailRequest();

            // when & then
            assertThatThrownBy(() -> applicantAdminService.sendResultMailToApplicants(
                    request, USERNAME, OTHER_CLUB_ID, "DOCUMENT"))
                    .isExactlyInstanceOf(NotClubAdminException.class);
            verifyNoInteractions(applyFormRepository, applicantRepository, emailService);
        }
    }

    @Nested
    @DisplayName("getApplicantPage(): 지원자 페이지 조회")
    class GetApplicantPageTest {

        @Test
        @DisplayName("활성 지원폼이 존재하면 지원자 페이지를 반환한다")
        void getApplicantPage_success() {
            // given
            ApplicantPageServiceRequest request = ApplicantPageServiceRequest.of(
                    USERNAME, "GRADE", false, 1, 7, "DOCUMENT");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.getId()).willReturn(APPLY_FORM_ID);
            given(applyForm.isHasInterview()).willReturn(true);
            given(applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(CLUB_ID, ACTIVE))
                    .willReturn(Optional.of(applyForm));

            ApplicantPageQueryResponse queryResponse = ApplicantPageQueryResponse.builder()
                    .currentPage(1)
                    .totalPage(1)
                    .totalCount(0)
                    .applicants(List.of())
                    .build();
            given(applicantRepository.findApplicantsPageWithSortCriteria(
                    "GRADE", false, 1, 7, APPLY_FORM_ID, "DOCUMENT"))
                    .willReturn(queryResponse);

            // when
            ApplicantPageServiceResponse response = applicantAdminService.getApplicantPage(request);

            // then
            assertThat(response.hasInterview()).isTrue();
            assertThat(response.currentPage()).isEqualTo(1);
            assertThat(response.totalCount()).isEqualTo(0);
            assertThat(response.applicants()).isEmpty();
        }

        @Test
        @DisplayName("활성 지원폼이 없으면 빈 응답을 반환한다")
        void getApplicantPage_returnsEmpty_whenNoActiveApplyForm() {
            // given
            ApplicantPageServiceRequest request = ApplicantPageServiceRequest.of(
                    USERNAME, "GRADE", false, 1, 7, "DOCUMENT");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(CLUB_ID, ACTIVE))
                    .willReturn(Optional.empty());

            // when
            ApplicantPageServiceResponse response = applicantAdminService.getApplicantPage(request);

            // then
            assertThat(response.hasInterview()).isNull();
            assertThat(response.applicants()).isEmpty();
            verify(applicantRepository, never()).findApplicantsPageWithSortCriteria(
                    anyString(), eq(false), eq(1), eq(7), anyString(), anyString());
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 NotClubAdminException이 발생한다")
        void getApplicantPage_throwsNotClubAdminException_whenNotClubAdmin() {
            // given
            ApplicantPageServiceRequest request = ApplicantPageServiceRequest.of(
                    USERNAME, "GRADE", false, 1, 7, "DOCUMENT");
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicantAdminService.getApplicantPage(request))
                    .isInstanceOf(NotClubAdminException.class);
        }
    }

    @Nested
    @DisplayName("getApplicantDetail(): 지원자 상세 조회")
    class GetApplicantDetailTest {

        @Test
        @DisplayName("정상적으로 지원자 상세 정보를 조회한다")
        void getApplicantDetail_success() {
            // given
            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            List<Answer> answers = List.of(
                    new Answer("질문1", null, null, true, List.of(), "답변1"));

            DocumentPhase documentPhase = mock(DocumentPhase.class);
            given(documentPhase.getAnswers()).willReturn(answers);
            given(documentPhase.getMemos()).willReturn(List.of());

            Applicant applicant = mock(Applicant.class);
            given(applicant.belongsToClub(CLUB_ID)).willReturn(true);
            given(applicant.getDocumentPhase()).willReturn(documentPhase);
            given(applicant.getName()).willReturn("홍길동");
            given(applicant.getAge()).willReturn(22);
            given(applicant.getMajor()).willReturn("컴퓨터공학과");
            given(applicant.getEmail()).willReturn("hong@test.com");
            given(applicant.getPhone()).willReturn("010-1234-5678");
            given(applicant.getStudentStatus()).willReturn(StudentStatus.ENROLLED);
            given(applicant.getGrade()).willReturn(Grade.FIRST_GRADE);
            given(applicant.getGender()).willReturn(Gender.MALE);

            given(applicantRepository.findByIdWithDocumentPhase(APPLICANT_ID))
                    .willReturn(Optional.of(applicant));

            // when
            var response = applicantAdminService.getApplicantDetail(USERNAME, APPLICANT_ID);

            // then
            assertThat(response.name()).isEqualTo("홍길동");
            assertThat(response.age()).isEqualTo(22);
            assertThat(response.major()).isEqualTo("컴퓨터공학과");
            assertThat(response.answers()).isEqualTo(answers);
            assertThat(response.memos()).isEmpty();
        }

        @Test
        @DisplayName("서류 단계 정보가 없으면 답변과 메모는 빈 리스트를 반환한다")
        void getApplicantDetail_returnsEmptyAnswersAndMemos_whenDocumentPhaseNull() {
            // given
            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            Applicant applicant = mock(Applicant.class);
            given(applicant.belongsToClub(CLUB_ID)).willReturn(true);
            given(applicant.getDocumentPhase()).willReturn(null);
            given(applicant.getName()).willReturn("홍길동");
            given(applicant.getAge()).willReturn(22);
            given(applicant.getMajor()).willReturn("컴퓨터공학과");
            given(applicant.getEmail()).willReturn("hong@test.com");
            given(applicant.getPhone()).willReturn("010-1234-5678");
            given(applicant.getStudentStatus()).willReturn(StudentStatus.ENROLLED);
            given(applicant.getGrade()).willReturn(Grade.FIRST_GRADE);
            given(applicant.getGender()).willReturn(Gender.MALE);

            given(applicantRepository.findByIdWithDocumentPhase(APPLICANT_ID))
                    .willReturn(Optional.of(applicant));

            // when
            var response = applicantAdminService.getApplicantDetail(USERNAME, APPLICANT_ID);

            // then
            assertThat(response.answers()).isEmpty();
            assertThat(response.memos()).isEmpty();
        }

        @Test
        @DisplayName("지원자가 존재하지 않으면 ApplicantNotFoundException이 발생한다")
        void getApplicantDetail_throwsApplicantNotFoundException() {
            // given
            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applicantRepository.findByIdWithDocumentPhase(APPLICANT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicantAdminService.getApplicantDetail(USERNAME, APPLICANT_ID))
                    .isInstanceOf(ApplicantNotFoundException.class);
        }

        @Test
        @DisplayName("지원자의 소속 동아리와 요청한 관리자의 동아리가 다르면 UnAuthorizedApplicantAccessException이 발생한다")
        void getApplicantDetail_throwsUnAuthorizedApplicantAccessException() {
            // given
            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            Applicant applicant = mock(Applicant.class);
            given(applicant.belongsToClub(CLUB_ID)).willReturn(false);

            given(applicantRepository.findByIdWithDocumentPhase(APPLICANT_ID))
                    .willReturn(Optional.of(applicant));

            // when & then
            assertThatThrownBy(() -> applicantAdminService.getApplicantDetail(USERNAME, APPLICANT_ID))
                    .isInstanceOf(UnAuthorizedApplicantAccessException.class);
        }
    }

    @Nested
    @DisplayName("searchApplicantByKeyword(): 이름으로 지원자 검색")
    class SearchApplicantByKeywordTest {

        @Test
        @DisplayName("정상적으로 검색 결과를 반환한다")
        void searchApplicantByKeyword_success() {
            // given
            ApplicantSearchServiceRequest request = ApplicantSearchServiceRequest.of(
                    USERNAME, "홍길동", "GRADE", false, 1, 7, "DOCUMENT");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.getId()).willReturn(APPLY_FORM_ID);
            given(applyForm.isHasInterview()).willReturn(false);
            given(applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(CLUB_ID, ACTIVE))
                    .willReturn(Optional.of(applyForm));

            ApplicantPageQueryResponse queryResponse = ApplicantPageQueryResponse.builder()
                    .currentPage(1)
                    .totalPage(1)
                    .totalCount(1)
                    .applicants(List.of())
                    .build();
            given(applicantRepository.searchApplicantsByKeyword(
                    "홍길동", "GRADE", false, 1, 7, APPLY_FORM_ID, "DOCUMENT"))
                    .willReturn(queryResponse);

            // when
            ApplicantPageServiceResponse response = applicantAdminService.searchApplicantByKeyword(request);

            // then
            assertThat(response.hasInterview()).isFalse();
            assertThat(response.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("활성 지원폼이 없으면 빈 응답을 반환한다")
        void searchApplicantByKeyword_returnsEmpty_whenNoActiveApplyForm() {
            // given
            ApplicantSearchServiceRequest request = ApplicantSearchServiceRequest.of(
                    USERNAME, "홍길동", "GRADE", false, 1, 7, "DOCUMENT");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(CLUB_ID, ACTIVE))
                    .willReturn(Optional.empty());

            // when
            ApplicantPageServiceResponse response = applicantAdminService.searchApplicantByKeyword(request);

            // then
            assertThat(response.applicants()).isEmpty();
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 NotClubAdminException이 발생한다")
        void searchApplicantByKeyword_throwsNotClubAdminException() {
            // given
            ApplicantSearchServiceRequest request = ApplicantSearchServiceRequest.of(
                    USERNAME, "홍길동", "GRADE", false, 1, 7, "DOCUMENT");
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicantAdminService.searchApplicantByKeyword(request))
                    .isInstanceOf(NotClubAdminException.class);
        }
    }

    @Nested
    @DisplayName("getApplicantsByStatus(): 합격/불합격 지원자 조회")
    class GetApplicantsByStatusTest {

        @Test
        @DisplayName("정상적으로 상태별 지원자 목록을 반환한다")
        void getApplicantsByStatus_success() {
            // given
            ApplicantStatusServiceRequest request = ApplicantStatusServiceRequest.of(
                    USERNAME, true, 1, 4, "DOCUMENT");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.getId()).willReturn(APPLY_FORM_ID);
            given(applyForm.isHasInterview()).willReturn(true);
            given(applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(CLUB_ID, ACTIVE))
                    .willReturn(Optional.of(applyForm));

            ApplicantPageQueryResponse queryResponse = ApplicantPageQueryResponse.builder()
                    .currentPage(1)
                    .totalPage(1)
                    .totalCount(2)
                    .applicants(List.of())
                    .build();
            given(applicantRepository.findApplicantsByStatus(true, 1, 4, APPLY_FORM_ID, "DOCUMENT"))
                    .willReturn(queryResponse);

            // when
            ApplicantPageServiceResponse response = applicantAdminService.getApplicantsByStatus(request);

            // then
            assertThat(response.totalCount()).isEqualTo(2);
            assertThat(response.hasInterview()).isTrue();
        }

        @Test
        @DisplayName("활성 지원폼이 없으면 빈 응답을 반환한다")
        void getApplicantsByStatus_returnsEmpty_whenNoActiveApplyForm() {
            // given
            ApplicantStatusServiceRequest request = ApplicantStatusServiceRequest.of(
                    USERNAME, false, 1, 4, "INTERVIEW");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(CLUB_ID, ACTIVE))
                    .willReturn(Optional.empty());

            // when
            ApplicantPageServiceResponse response = applicantAdminService.getApplicantsByStatus(request);

            // then
            assertThat(response.applicants()).isEmpty();
            verify(applicantRepository, never()).findApplicantsByStatus(
                    anyBoolean(), eq(1), eq(4), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("updateApplicantStatus(): 지원자 전형 상태 변경")
    class UpdateApplicantStatusTest {

        @ParameterizedTest(name = "{0} 전형을 {1} 상태로 변경한다")
        @CsvSource({
                "DOCUMENT, PASS",
                "DOCUMENT, FAIL",
                "DOCUMENT, EVALUATING",
                "INTERVIEW, PASS",
                "INTERVIEW, FAIL",
                "INTERVIEW, EVALUATING"
        })
        @DisplayName("자기 동아리 지원자의 전형 상태를 변경한다")
        void updateApplicantStatus_success(ApplicantPhase phase, PhaseStatus status) {
            // given
            Club club = createClub(CLUB_ID);
            Applicant applicant = mock(Applicant.class);
            given(applicant.belongsToClub(CLUB_ID)).willReturn(true);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            StatusUpdateServiceRequest request = StatusUpdateServiceRequest.of(
                    USERNAME, APPLICANT_ID, status, phase.name());

            // when
            applicantAdminService.updateApplicantStatus(request);

            // then
            verify(applicant).changeEvaluationStatus(phase, status);
        }

        @Test
        @DisplayName("지원자가 존재하지 않으면 ApplicantNotFoundException이 발생한다")
        void updateApplicantStatus_throwsApplicantNotFoundException() {
            // given
            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.empty());

            StatusUpdateServiceRequest request = StatusUpdateServiceRequest.of(
                    USERNAME, APPLICANT_ID, PhaseStatus.PASS, "DOCUMENT");

            // when & then
            assertThatThrownBy(() -> applicantAdminService.updateApplicantStatus(request))
                    .isInstanceOf(ApplicantNotFoundException.class);
        }

        @Test
        @DisplayName("지원자의 소속 동아리가 다르면 UnAuthorizedApplicantAccessException이 발생한다")
        void updateApplicantStatus_throwsUnAuthorizedApplicantAccessException() {
            // given
            Club club = createClub(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            Applicant applicant = mock(Applicant.class);
            given(applicant.belongsToClub(CLUB_ID)).willReturn(false);
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));

            StatusUpdateServiceRequest request = StatusUpdateServiceRequest.of(
                    USERNAME, APPLICANT_ID, PhaseStatus.PASS, "DOCUMENT");

            // when & then
            assertThatThrownBy(() -> applicantAdminService.updateApplicantStatus(request))
                    .isInstanceOf(UnAuthorizedApplicantAccessException.class);
            verify(applicant, never()).changeEvaluationStatus(any(), any());
            verifyNoInteractions(clubMemberRepository, emailService);
        }
    }

    @Nested
    @DisplayName("finalizeApplicantsStatus(): 전형 최종 마감 처리")
    class FinalizeApplicantsStatusTest {

        @Captor
        private ArgumentCaptor<List<ClubMember>> clubMembersCaptor;

        @Test
        @DisplayName("동아리가 없으면 ClubNotFoundException이 발생하고 후속 처리를 하지 않는다")
        void finalizeApplicantsStatus_throwsClubNotFoundException() {
            // given
            given(clubRepository.findById(CLUB_ID))
                    .willReturn(Optional.empty());

            ApplicantFinalizationRequest request = ApplicantFinalizationRequest.of(USERNAME, CLUB_ID, "DOCUMENT");

            // when & then
            assertThatThrownBy(() -> applicantAdminService.finalizeApplicantsStatus(request))
                    .isExactlyInstanceOf(ClubNotFoundException.class);
            verifyNoInteractions(
                    applicantRepository,
                    applyFormRepository,
                    emailService,
                    clubMemberRepository
            );
        }

        @Test
        @DisplayName("서류 전형 마감 시, 면접이 있는 지원폼이면 합격자를 면접 단계로 이동시킨다")
        void finalizeApplicantsStatus_document_movesToInterviewPhase() {
            // given
            ApplicantFinalizationRequest request = ApplicantFinalizationRequest.of(USERNAME, CLUB_ID, "DOCUMENT");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            LocalDate interviewStartDate = LocalDate.of(2026, 8, 1);
            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.getId()).willReturn(APPLY_FORM_ID);
            given(applyForm.isHasInterview()).willReturn(true);
            given(applyForm.getInterviewStartDate()).willReturn(interviewStartDate);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(applyForm));

            Applicant passedApplicant = mock(Applicant.class);
            given(passedApplicant.statusOf(ApplicantPhase.DOCUMENT)).willReturn(Optional.of(PhaseStatus.PASS));
            given(passedApplicant.isInInterviewPhase()).willReturn(false);

            Applicant failedApplicant = mock(Applicant.class);
            given(failedApplicant.statusOf(ApplicantPhase.DOCUMENT)).willReturn(Optional.of(PhaseStatus.FAIL));

            given(applicantRepository.findByApplyFormId(APPLY_FORM_ID))
                    .willReturn(List.of(passedApplicant, failedApplicant));

            // when
            ApplicantFinalizeServiceResponse response = applicantAdminService.finalizeApplicantsStatus(request);

            // then
            assertThat(response.passedCount()).isEqualTo(1);
            assertThat(response.totalFinalizedCount()).isEqualTo(2);
            verify(passedApplicant).updateToInterviewPhase(interviewStartDate);
            verify(clubMemberRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("면접 전형 마감 시, 합격자를 동아리 부원으로 등록한다")
        void finalizeApplicantsStatus_interview_savesClubMembers() {
            // given
            ApplicantFinalizationRequest request = ApplicantFinalizationRequest.of(USERNAME, CLUB_ID, "INTERVIEW");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.getId()).willReturn(APPLY_FORM_ID);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(applyForm));

            Applicant passedApplicant = mock(Applicant.class);
            given(passedApplicant.statusOf(ApplicantPhase.INTERVIEW)).willReturn(Optional.of(PhaseStatus.PASS));
            given(passedApplicant.getName()).willReturn("홍길동");
            given(passedApplicant.getGrade()).willReturn(Grade.FIRST_GRADE);
            given(passedApplicant.getMajor()).willReturn("컴퓨터공학과");
            given(passedApplicant.getEmail()).willReturn("hong@test.com");
            given(passedApplicant.getPhone()).willReturn("010-1234-5678");
            given(passedApplicant.getGender()).willReturn(Gender.MALE);

            given(applicantRepository.findByApplyFormId(APPLY_FORM_ID)).willReturn(List.of(passedApplicant));
            given(clubMemberRepository.existsByClubIdAndEmail(CLUB_ID, "hong@test.com")).willReturn(false);

            // when
            ApplicantFinalizeServiceResponse response = applicantAdminService.finalizeApplicantsStatus(request);

            // then
            assertThat(response.passedCount()).isEqualTo(1);
            verify(clubMemberRepository).saveAll(clubMembersCaptor.capture());
            assertThat(clubMembersCaptor.getValue()).singleElement().satisfies(member -> {
                assertThat(member.getClub()).isSameAs(club);
                assertThat(member.getEmail()).isEqualTo("hong@test.com");
                assertThat(member.getRole()).isEqualTo(MemberRole.MEMBER);
            });
        }

        @Test
        @DisplayName("이미 부원으로 등록된 지원자는 다시 등록하지 않는다")
        void finalizeApplicantsStatus_skipsAlreadyRegisteredMember() {
            // given
            ApplicantFinalizationRequest request = ApplicantFinalizationRequest.of(USERNAME, CLUB_ID, "INTERVIEW");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.getId()).willReturn(APPLY_FORM_ID);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(applyForm));

            Applicant passedApplicant = mock(Applicant.class);
            given(passedApplicant.statusOf(ApplicantPhase.INTERVIEW)).willReturn(Optional.of(PhaseStatus.PASS));
            given(passedApplicant.getEmail()).willReturn("hong@test.com");

            given(applicantRepository.findByApplyFormId(APPLY_FORM_ID)).willReturn(List.of(passedApplicant));
            given(clubMemberRepository.existsByClubIdAndEmail(CLUB_ID, "hong@test.com")).willReturn(true);

            // when
            applicantAdminService.finalizeApplicantsStatus(request);

            // then
            verify(clubMemberRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("활성 지원폼이 없으면 ActiveApplyFormNotFoundException이 발생한다")
        void finalizeApplicantsStatus_throwsActiveApplyFormNotFoundException() {
            // given
            ApplicantFinalizationRequest request = ApplicantFinalizationRequest.of(USERNAME, CLUB_ID, "DOCUMENT");

            Club club = createClub(CLUB_ID);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicantAdminService.finalizeApplicantsStatus(request))
                    .isInstanceOf(ActiveApplyFormNotFoundException.class);
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 NotClubAdminException이 발생한다")
        void finalizeApplicantsStatus_throwsNotClubAdminException() {
            // given
            Club club = createClub(CLUB_ID);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ApplicantFinalizationRequest request = ApplicantFinalizationRequest.of("otherAdmin", CLUB_ID, "DOCUMENT");

            // when & then
            assertThatThrownBy(() -> applicantAdminService.finalizeApplicantsStatus(request))
                    .isInstanceOf(NotClubAdminException.class);
        }
    }

    @Nested
    @DisplayName("sendResultMailToApplicants(): 합불 결과 메일 발송")
    class SendResultMailToApplicantsTest {

        @Test
        @DisplayName("합격자와 불합격자 각각에게 결과 메일을 발송한다")
        void sendResultMailToApplicants_success() {
            // given
            SendResultMailServiceRequest request = createResultMailRequest();

            Club club = createClub(CLUB_ID);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.getId()).willReturn(APPLY_FORM_ID);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(applyForm));

            Applicant passedApplicant = mock(Applicant.class);
            given(passedApplicant.statusOf(ApplicantPhase.DOCUMENT)).willReturn(Optional.of(PhaseStatus.PASS));
            given(passedApplicant.getEmail()).willReturn("passed@test.com");

            Applicant failedApplicant = mock(Applicant.class);
            given(failedApplicant.statusOf(ApplicantPhase.DOCUMENT)).willReturn(Optional.of(PhaseStatus.FAIL));
            given(failedApplicant.getEmail()).willReturn("failed@test.com");

            given(applicantRepository.findByApplyFormId(APPLY_FORM_ID))
                    .willReturn(List.of(passedApplicant, failedApplicant));

            // when
            applicantAdminService.sendResultMailToApplicants(request, USERNAME, CLUB_ID, "DOCUMENT");

            // then
            verify(emailService).sendResultMail(List.of("passed@test.com"), request.pass());
            verify(emailService).sendResultMail(List.of("failed@test.com"), request.fail());
        }

        @Test
        @DisplayName("활성 지원폼이 없으면 ActiveApplyFormNotFoundException이 발생한다")
        void sendResultMailToApplicants_throwsActiveApplyFormNotFoundException() {
            // given
            SendResultMailServiceRequest request = createResultMailRequest();

            Club club = createClub(CLUB_ID);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    applicantAdminService.sendResultMailToApplicants(request, USERNAME, CLUB_ID, "DOCUMENT"))
                    .isInstanceOf(ActiveApplyFormNotFoundException.class);
            verify(emailService, never()).sendResultMail(anyList(), any());
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 NotClubAdminException이 발생한다")
        void sendResultMailToApplicants_throwsNotClubAdminException() {
            // given
            SendResultMailServiceRequest request = createResultMailRequest();

            Club club = createClub(CLUB_ID, OTHER_USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            // when & then
            assertThatThrownBy(() ->
                    applicantAdminService.sendResultMailToApplicants(request, USERNAME, CLUB_ID, "DOCUMENT"))
                    .isInstanceOf(NotClubAdminException.class);
            verifyNoInteractions(applyFormRepository, applicantRepository, emailService);
        }

        @Test
        @DisplayName("동아리가 없으면 ClubNotFoundException이 발생하고 메일을 발송하지 않는다")
        void sendResultMailToApplicants_clubNotFound() {
            // given
            SendResultMailServiceRequest request = createResultMailRequest();
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicantAdminService.sendResultMailToApplicants(
                    request, USERNAME, CLUB_ID, "DOCUMENT"))
                    .isExactlyInstanceOf(ClubNotFoundException.class);
            verifyNoInteractions(applyFormRepository, applicantRepository, emailService);
        }
    }
}
