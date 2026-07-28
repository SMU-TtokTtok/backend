package org.project.ttokttok.domain.applicant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applicant.service.answer.AnswerAssembler;
import org.project.ttokttok.domain.applicant.service.answer.FileAnswerUploader;
import org.project.ttokttok.domain.applicant.controller.dto.request.AnswerRequest;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplyFormRequest;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.exception.AlreadyApplicantExistsException;
import org.project.ttokttok.domain.applicant.exception.AnswerRequestNotMatchException;
import org.project.ttokttok.domain.applicant.exception.ListSizeNotMatchException;
import org.project.ttokttok.domain.applicant.exception.QuestionParseFailException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.repository.dto.UserApplicationHistoryQueryResponse;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.project.ttokttok.domain.user.exception.UserNotFoundException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

@ExtendWith(MockitoExtension.class)
class ApplicantUserServiceTest {

    private ApplicantUserService applicantUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private ApplyFormRepository applyFormRepository;

    @Mock
    private TempApplicantRepository tempApplicantRepository;

    @Mock
    private S3Service s3Service;

    /**
     * 답변 조립기는 실제 구현을 사용한다.
     * 파일 답변 처리 동작을 계속 검증하기 위해 S3Service만 목으로 두고 그 위 계층은 실물로 조립한다.
     */
    @BeforeEach
    void setUp() {
        AnswerAssembler answerAssembler = new AnswerAssembler(new FileAnswerUploader(s3Service));

        applicantUserService = new ApplicantUserService(
                userRepository,
                applicantRepository,
                applyFormRepository,
                tempApplicantRepository,
                answerAssembler
        );
    }

    private static final String EMAIL = "user@sangmyung.kr";
    private static final String CLUB_ID = "club-1";
    private static final String FORM_ID = "form-1";
    private static final String APPLICANT_ID = "applicant-1";

    private ApplyFormRequest createApplyFormRequest(List<AnswerRequest> answers) {
        return new ApplyFormRequest(
                "홍길동",
                22,
                "컴퓨터공학과",
                "hong@test.com",
                "010-1234-5678",
                StudentStatus.ENROLLED,
                Grade.FIRST_GRADE,
                Gender.MALE,
                FORM_ID,
                answers
        );
    }

    @Nested
    @DisplayName("apply(): 지원서 제출")
    class ApplyTest {

        @Test
        @DisplayName("파일 질문이 없는 경우 정상적으로 지원서를 제출한다")
        void apply_success_withoutFileQuestion() {
            // given
            Question question = new Question("q1", "질문1", null, QuestionType.SHORT_ANSWER, true, List.of());
            ApplyForm form = mock(ApplyForm.class);
            given(form.getId()).willReturn(FORM_ID);
            given(form.getFormJson()).willReturn(List.of(question));

            ApplyFormRequest request = createApplyFormRequest(List.of(new AnswerRequest("q1", "답변1")));

            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(form));
            given(applicantRepository.existsByUserEmailAndApplyFormId(EMAIL, FORM_ID)).willReturn(false);
            given(tempApplicantRepository.findByUserEmailAndFormId(EMAIL, FORM_ID)).willReturn(Optional.empty());

            Applicant savedApplicant = mock(Applicant.class);
            given(savedApplicant.getId()).willReturn(APPLICANT_ID);
            given(applicantRepository.save(any(Applicant.class))).willReturn(savedApplicant);

            // when
            String result = applicantUserService.apply(EMAIL, request, null, null, CLUB_ID);

            // then
            assertThat(result).isEqualTo(APPLICANT_ID);
            verify(applicantRepository).save(any(Applicant.class));
            verify(s3Service, never()).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("파일 질문이 있는 경우 S3에 파일을 업로드하고 지원서를 제출한다")
        void apply_success_withFileUpload() {
            // given
            Question question = new Question("q2", "파일질문", null, QuestionType.FILE, true, List.of());
            ApplyForm form = mock(ApplyForm.class);
            given(form.getId()).willReturn(FORM_ID);
            given(form.getFormJson()).willReturn(List.of(question));

            ApplyFormRequest request = createApplyFormRequest(List.of(new AnswerRequest("q2", null)));

            MultipartFile file = new MockMultipartFile("q2", "resume.pdf", "application/pdf", "content".getBytes());

            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(form));
            given(applicantRepository.existsByUserEmailAndApplyFormId(EMAIL, FORM_ID)).willReturn(false);
            given(tempApplicantRepository.findByUserEmailAndFormId(EMAIL, FORM_ID)).willReturn(Optional.empty());
            given(s3Service.uploadFile(file, "applicant/" + EMAIL + "/")).willReturn("https://s3/resume.pdf");

            Applicant savedApplicant = mock(Applicant.class);
            given(savedApplicant.getId()).willReturn(APPLICANT_ID);
            given(applicantRepository.save(any(Applicant.class))).willReturn(savedApplicant);

            // when
            String result = applicantUserService.apply(EMAIL, request, List.of("q2"), List.of(file), CLUB_ID);

            // then
            assertThat(result).isEqualTo(APPLICANT_ID);
            verify(s3Service, times(1)).uploadFile(file, "applicant/" + EMAIL + "/");
        }

        @Test
        @DisplayName("동일한 지원폼에 이미 임시 지원폼이 존재하면 삭제한다")
        void apply_deletesTempApplicant_whenExists() {
            // given
            Question question = new Question("q1", "질문1", null, QuestionType.SHORT_ANSWER, true, List.of());
            ApplyForm form = mock(ApplyForm.class);
            given(form.getId()).willReturn(FORM_ID);
            given(form.getFormJson()).willReturn(List.of(question));

            ApplyFormRequest request = createApplyFormRequest(List.of(new AnswerRequest("q1", "답변1")));

            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(form));
            given(applicantRepository.existsByUserEmailAndApplyFormId(EMAIL, FORM_ID)).willReturn(false);

            TempApplicant tempApplicant = mock(TempApplicant.class);
            given(tempApplicantRepository.findByUserEmailAndFormId(EMAIL, FORM_ID))
                    .willReturn(Optional.of(tempApplicant));

            Applicant savedApplicant = mock(Applicant.class);
            given(savedApplicant.getId()).willReturn(APPLICANT_ID);
            given(applicantRepository.save(any(Applicant.class))).willReturn(savedApplicant);

            // when
            applicantUserService.apply(EMAIL, request, null, null, CLUB_ID);

            // then
            verify(tempApplicantRepository).delete(tempApplicant);
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 UserNotFoundException이 발생한다")
        void apply_throwsUserNotFoundException_whenUserDoesNotExist() {
            // given
            ApplyFormRequest request = createApplyFormRequest(List.of());
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> applicantUserService.apply(EMAIL, request, null, null, CLUB_ID))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("활성화된 지원폼이 없으면 ApplyFormNotFoundException이 발생한다")
        void apply_throwsApplyFormNotFoundException_whenNoActiveForm() {
            // given
            ApplyFormRequest request = createApplyFormRequest(List.of());
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicantUserService.apply(EMAIL, request, null, null, CLUB_ID))
                    .isInstanceOf(ApplyFormNotFoundException.class);
        }

        @Test
        @DisplayName("이미 지원한 이력이 있으면 AlreadyApplicantExistsException이 발생한다")
        void apply_throwsAlreadyApplicantExistsException_whenDuplicateApply() {
            // given
            ApplyForm form = mock(ApplyForm.class);
            given(form.getId()).willReturn(FORM_ID);

            ApplyFormRequest request = createApplyFormRequest(List.of());
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(form));
            given(applicantRepository.existsByUserEmailAndApplyFormId(EMAIL, FORM_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> applicantUserService.apply(EMAIL, request, null, null, CLUB_ID))
                    .isInstanceOf(AlreadyApplicantExistsException.class);
        }

        @Test
        @DisplayName("필수 파일 질문에 파일이 없으면 AnswerRequestNotMatchException이 발생한다")
        void apply_throwsAnswerRequestNotMatchException_whenRequiredFileQuestionMissing() {
            // given
            Question fileQuestion = new Question("q2", "파일질문", null, QuestionType.FILE, true, List.of());
            ApplyForm form = mock(ApplyForm.class);
            given(form.getId()).willReturn(FORM_ID);
            given(form.getFormJson()).willReturn(List.of(fileQuestion));

            ApplyFormRequest request = createApplyFormRequest(List.of());
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(form));
            given(applicantRepository.existsByUserEmailAndApplyFormId(EMAIL, FORM_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> applicantUserService.apply(EMAIL, request, null, null, CLUB_ID))
                    .isInstanceOf(AnswerRequestNotMatchException.class);
        }

        @Test
        @DisplayName("파일 질문 개수와 파일 개수가 일치하지 않으면 ListSizeNotMatchException이 발생한다")
        void apply_throwsListSizeNotMatchException_whenFileCountMismatch() {
            // given
            Question fileQuestion1 = new Question("q2", "파일질문1", null, QuestionType.FILE, true, List.of());
            Question fileQuestion2 = new Question("q3", "파일질문2", null, QuestionType.FILE, true, List.of());
            ApplyForm form = mock(ApplyForm.class);
            given(form.getId()).willReturn(FORM_ID);
            given(form.getFormJson()).willReturn(List.of(fileQuestion1, fileQuestion2));

            ApplyFormRequest request = createApplyFormRequest(List.of());
            MultipartFile file = new MockMultipartFile("q2", "resume.pdf", "application/pdf", "content".getBytes());

            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(form));
            given(applicantRepository.existsByUserEmailAndApplyFormId(EMAIL, FORM_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> applicantUserService.apply(
                    EMAIL, request, List.of("q2", "q3"), List.of(file), CLUB_ID))
                    .isInstanceOf(ListSizeNotMatchException.class);
        }

        @Test
        @DisplayName("답변의 질문 ID가 지원폼에 존재하지 않으면 QuestionParseFailException이 발생한다")
        void apply_throwsQuestionParseFailException_whenQuestionIdNotFound() {
            // given
            Question question = new Question("q1", "질문1", null, QuestionType.SHORT_ANSWER, true, List.of());
            ApplyForm form = mock(ApplyForm.class);
            given(form.getId()).willReturn(FORM_ID);
            given(form.getFormJson()).willReturn(List.of(question));

            ApplyFormRequest request = createApplyFormRequest(List.of(new AnswerRequest("존재하지-않는-id", "답변")));

            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applyFormRepository.findByClubIdAndStatus(CLUB_ID, ACTIVE)).willReturn(Optional.of(form));
            given(applicantRepository.existsByUserEmailAndApplyFormId(EMAIL, FORM_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> applicantUserService.apply(EMAIL, request, null, null, CLUB_ID))
                    .isInstanceOf(QuestionParseFailException.class);
        }
    }

    @Nested
    @DisplayName("getUserApplicationHistory(): 사용자 지원내역 조회")
    class GetUserApplicationHistoryTest {

        private UserApplicationHistoryQueryResponse createResponse(String applicantId, LocalDate applyEndDate) {
            return new UserApplicationHistoryQueryResponse(
                    applicantId,
                    "club-" + applicantId,
                    "동아리 " + applicantId,
                    ClubType.CENTRAL,
                    ClubCategory.ACADEMIC,
                    "",
                    "한줄 소개",
                    null,
                    3,
                    true,
                    false,
                    ApplicantPhase.DOCUMENT,
                    LocalDateTime.now(),
                    applyEndDate
            );
        }

        @Test
        @DisplayName("조회 결과가 size보다 많으면 hasNext가 true이고 다음 커서를 반환한다")
        void getUserApplicationHistory_hasNext_true() {
            // given
            int size = 2;
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applicantRepository.getUserApplicationHistory(EMAIL, size, null, "latest"))
                    .willReturn(List.of(
                            createResponse("a1", null),
                            createResponse("a2", null),
                            createResponse("a3", null)
                    ));

            // when
            ClubListServiceResponse response = applicantUserService.getUserApplicationHistory(
                    EMAIL, size, null, "latest");

            // then
            assertThat(response.clubs()).hasSize(2);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo("a2");
        }

        @Test
        @DisplayName("조회 결과가 size 이하면 hasNext가 false이고 다음 커서가 없다")
        void getUserApplicationHistory_hasNext_false() {
            // given
            int size = 2;
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applicantRepository.getUserApplicationHistory(EMAIL, size, null, "latest"))
                    .willReturn(List.of(createResponse("a1", null)));

            // when
            ClubListServiceResponse response = applicantUserService.getUserApplicationHistory(
                    EMAIL, size, null, "latest");

            // then
            assertThat(response.clubs()).hasSize(1);
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 UserNotFoundException이 발생한다")
        void getUserApplicationHistory_throwsUserNotFoundException() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> applicantUserService.getUserApplicationHistory(EMAIL, 10, null, "latest"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("마감일이 일주일 이내면 마감 임박으로 표시한다")
        void getUserApplicationHistory_marksDeadlineImminent_withinAWeek() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applicantRepository.getUserApplicationHistory(eq(EMAIL), eq(10), eq((String) null), eq("latest")))
                    .willReturn(List.of(createResponse("a1", LocalDate.now().plusDays(3))));

            // when
            ClubListServiceResponse response = applicantUserService.getUserApplicationHistory(
                    EMAIL, 10, null, "latest");

            // then
            assertThat(response.clubs().get(0).isDeadlineImminent()).isTrue();
        }

        @Test
        @DisplayName("마감일이 일주일 이상 남았으면 마감 임박이 아니다")
        void getUserApplicationHistory_doesNotMarkDeadlineImminent_whenFarAway() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applicantRepository.getUserApplicationHistory(eq(EMAIL), eq(10), eq((String) null), eq("latest")))
                    .willReturn(List.of(createResponse("a1", LocalDate.now().plusDays(10))));

            // when
            ClubListServiceResponse response = applicantUserService.getUserApplicationHistory(
                    EMAIL, 10, null, "latest");

            // then
            assertThat(response.clubs().get(0).isDeadlineImminent()).isFalse();
        }

        @Test
        @DisplayName("마감일 정보가 없으면 마감 임박이 아니다")
        void getUserApplicationHistory_doesNotMarkDeadlineImminent_whenApplyEndDateNull() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);
            given(applicantRepository.getUserApplicationHistory(eq(EMAIL), eq(10), eq((String) null), eq("latest")))
                    .willReturn(List.of(createResponse("a1", null)));

            // when
            ClubListServiceResponse response = applicantUserService.getUserApplicationHistory(
                    EMAIL, 10, null, "latest");

            // then
            assertThat(response.clubs().get(0).isDeadlineImminent()).isFalse();
        }
    }
}
