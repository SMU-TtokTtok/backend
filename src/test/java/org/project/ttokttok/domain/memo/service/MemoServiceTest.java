package org.project.ttokttok.domain.memo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.DocumentPhase;
import org.project.ttokttok.domain.applicant.exception.ApplicantNotFoundException;
import org.project.ttokttok.domain.applicant.exception.UnAuthorizedApplicantAccessException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.memo.service.dto.request.CreateMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.DeleteMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.UpdateMemoServiceRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private MemoService memoService;

    private static final String USERNAME = "admin1234";
    private static final String CLUB_ID = "club-1";
    private static final String APPLICANT_ID = "applicant-1";
    private static final String CONTENT = "면접 태도가 좋았음";

    // 소속 동아리 id가 clubId인 지원자를 만든다. documentPhase는 별도로 stub하지 않는 한 null이다.
    private Applicant applicantInClub(String clubId) {
        Applicant applicant = mock(Applicant.class);
        ApplyForm applyForm = mock(ApplyForm.class);
        Club applicantClub = mock(Club.class);
        given(applicantClub.getId()).willReturn(clubId);
        given(applyForm.getClub()).willReturn(applicantClub);
        given(applicant.getApplyForm()).willReturn(applyForm);
        return applicant;
    }

    @Nested
    @DisplayName("createMemo 테스트")
    class CreateMemoTest {

        @Test
        @DisplayName("정상 요청이면 메모를 생성하고 memoId를 반환한다")
        void createMemo_Success() {
            // given
            Club club = mock(Club.class);
            given(club.getId()).willReturn(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            DocumentPhase documentPhase = mock(DocumentPhase.class);
            given(documentPhase.addMemo(CONTENT)).willReturn("memo-1");
            Applicant applicant = applicantInClub(CLUB_ID);
            given(applicant.getDocumentPhase()).willReturn(documentPhase);
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));

            CreateMemoServiceRequest request = CreateMemoServiceRequest.of(USERNAME, APPLICANT_ID, CONTENT);

            // when
            String memoId = memoService.createMemo(USERNAME, request);

            // then
            assertThat(memoId).isEqualTo("memo-1");
            verify(documentPhase, times(1)).addMemo(CONTENT);
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 NotClubAdminException이 발생한다")
        void createMemo_Fail_NotClubAdmin() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());
            CreateMemoServiceRequest request = CreateMemoServiceRequest.of(USERNAME, APPLICANT_ID, CONTENT);

            // when & then
            assertThatThrownBy(() -> memoService.createMemo(USERNAME, request))
                    .isInstanceOf(NotClubAdminException.class);

            verify(applicantRepository, never()).findById(APPLICANT_ID);
        }

        @Test
        @DisplayName("지원자를 찾을 수 없으면 ApplicantNotFoundException이 발생한다")
        void createMemo_Fail_ApplicantNotFound() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(mock(Club.class)));
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.empty());
            CreateMemoServiceRequest request = CreateMemoServiceRequest.of(USERNAME, APPLICANT_ID, CONTENT);

            // when & then
            assertThatThrownBy(() -> memoService.createMemo(USERNAME, request))
                    .isInstanceOf(ApplicantNotFoundException.class);
        }

        @Test
        @DisplayName("지원자가 다른 동아리 소속이면 UnAuthorizedApplicantAccessException이 발생한다")
        void createMemo_Fail_UnAuthorizedAccess() {
            // given
            Club club = mock(Club.class);
            given(club.getId()).willReturn(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            Applicant applicant = applicantInClub("other-club");
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            CreateMemoServiceRequest request = CreateMemoServiceRequest.of(USERNAME, APPLICANT_ID, CONTENT);

            // when & then
            assertThatThrownBy(() -> memoService.createMemo(USERNAME, request))
                    .isInstanceOf(UnAuthorizedApplicantAccessException.class);
        }

        @Test
        @DisplayName("서류 단계가 없는 지원자면 IllegalArgumentException이 발생한다")
        void createMemo_Fail_NoDocumentPhase() {
            // given
            Club club = mock(Club.class);
            given(club.getId()).willReturn(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            Applicant applicant = applicantInClub(CLUB_ID); // documentPhase는 stub하지 않아 기본값 null
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            CreateMemoServiceRequest request = CreateMemoServiceRequest.of(USERNAME, APPLICANT_ID, CONTENT);

            // when & then
            assertThatThrownBy(() -> memoService.createMemo(USERNAME, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메모는 서류 지원자에만 작성할 수 있습니다.");
        }
    }

    @Nested
    @DisplayName("updateMemo 테스트")
    class UpdateMemoTest {

        @Test
        @DisplayName("정상 요청이면 documentPhase.updateMemo에 위임한다")
        void updateMemo_Success() {
            // given
            Club club = mock(Club.class);
            given(club.getId()).willReturn(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            DocumentPhase documentPhase = mock(DocumentPhase.class);
            Applicant applicant = applicantInClub(CLUB_ID);
            given(applicant.getDocumentPhase()).willReturn(documentPhase);
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));

            UpdateMemoServiceRequest request = UpdateMemoServiceRequest.of("memo-1", USERNAME, APPLICANT_ID, CONTENT);

            // when
            memoService.updateMemo(USERNAME, request);

            // then
            verify(documentPhase, times(1)).updateMemo("memo-1", CONTENT);
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 NotClubAdminException이 발생한다")
        void updateMemo_Fail_NotClubAdmin() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.empty());
            UpdateMemoServiceRequest request = UpdateMemoServiceRequest.of("memo-1", USERNAME, APPLICANT_ID, CONTENT);

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(USERNAME, request))
                    .isInstanceOf(NotClubAdminException.class);
        }

        @Test
        @DisplayName("지원자가 다른 동아리 소속이면 UnAuthorizedApplicantAccessException이 발생한다")
        void updateMemo_Fail_UnAuthorizedAccess() {
            // given
            Club club = mock(Club.class);
            given(club.getId()).willReturn(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            Applicant applicant = applicantInClub("other-club");
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            UpdateMemoServiceRequest request = UpdateMemoServiceRequest.of("memo-1", USERNAME, APPLICANT_ID, CONTENT);

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(USERNAME, request))
                    .isInstanceOf(UnAuthorizedApplicantAccessException.class);
        }

        @Test
        @DisplayName("서류 단계가 없는 지원자면 IllegalArgumentException이 발생한다")
        void updateMemo_Fail_NoDocumentPhase() {
            // given
            Club club = mock(Club.class);
            given(club.getId()).willReturn(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            Applicant applicant = applicantInClub(CLUB_ID);
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            UpdateMemoServiceRequest request = UpdateMemoServiceRequest.of("memo-1", USERNAME, APPLICANT_ID, CONTENT);

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(USERNAME, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메모는 서류 지원자에만 수정할 수 있습니다.");
        }
    }

    @Nested
    @DisplayName("deleteMemo 테스트")
    class DeleteMemoTest {

        @Test
        @DisplayName("정상 요청이면 documentPhase.deleteMemo에 위임한다")
        void deleteMemo_Success() {
            // given
            Club club = mock(Club.class);
            given(club.getId()).willReturn(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));

            DocumentPhase documentPhase = mock(DocumentPhase.class);
            Applicant applicant = applicantInClub(CLUB_ID);
            given(applicant.getDocumentPhase()).willReturn(documentPhase);
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));

            DeleteMemoServiceRequest request = DeleteMemoServiceRequest.of("memo-1", APPLICANT_ID, USERNAME);

            // when
            memoService.deleteMemo(USERNAME, request);

            // then
            verify(documentPhase, times(1)).deleteMemo("memo-1");
        }

        @Test
        @DisplayName("지원자를 찾을 수 없으면 ApplicantNotFoundException이 발생한다")
        void deleteMemo_Fail_ApplicantNotFound() {
            // given
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(mock(Club.class)));
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.empty());
            DeleteMemoServiceRequest request = DeleteMemoServiceRequest.of("memo-1", APPLICANT_ID, USERNAME);

            // when & then
            assertThatThrownBy(() -> memoService.deleteMemo(USERNAME, request))
                    .isInstanceOf(ApplicantNotFoundException.class);
        }

        @Test
        @DisplayName("지원자가 다른 동아리 소속이면 UnAuthorizedApplicantAccessException이 발생한다")
        void deleteMemo_Fail_UnAuthorizedAccess() {
            // given
            Club club = mock(Club.class);
            given(club.getId()).willReturn(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            Applicant applicant = applicantInClub("other-club");
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            DeleteMemoServiceRequest request = DeleteMemoServiceRequest.of("memo-1", APPLICANT_ID, USERNAME);

            // when & then
            assertThatThrownBy(() -> memoService.deleteMemo(USERNAME, request))
                    .isInstanceOf(UnAuthorizedApplicantAccessException.class);
        }

        @Test
        @DisplayName("서류 단계가 없는 지원자면 IllegalArgumentException이 발생한다")
        void deleteMemo_Fail_NoDocumentPhase() {
            // given
            Club club = mock(Club.class);
            given(club.getId()).willReturn(CLUB_ID);
            given(clubRepository.findByAdminUsername(USERNAME)).willReturn(Optional.of(club));
            Applicant applicant = applicantInClub(CLUB_ID);
            given(applicantRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            DeleteMemoServiceRequest request = DeleteMemoServiceRequest.of("memo-1", APPLICANT_ID, USERNAME);

            // when & then
            assertThatThrownBy(() -> memoService.deleteMemo(USERNAME, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메모는 서류 지원자에만 삭제할 수 있습니다.");
        }
    }
}
