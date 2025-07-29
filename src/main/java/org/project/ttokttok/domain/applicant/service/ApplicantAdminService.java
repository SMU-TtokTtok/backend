package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.Status;
import org.project.ttokttok.domain.applicant.exception.ApplicantNotFoundException;
import org.project.ttokttok.domain.applicant.exception.UnAuthorizedApplicantAccessException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.service.dto.request.*;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantDetailServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantFinalizeServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantPageServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.MemoResponse;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.exception.ActiveApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicantAdminService {

    private final ApplicantRepository applicantRepository;
    private final ApplyFormRepository applyFormRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ApplicantPageServiceResponse getApplicantPage(ApplicantPageServiceRequest request) {
        // 1. username으로 관리하는 동아리 찾기
        // 존재하지 않으면 관리자 찾기 실패.
        Club club = clubRepository.findByAdminUsername(request.username())
                .orElseThrow(NotClubAdminException::new);

        // 2. 가장 최신의 지원 폼 찾기
        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(club.getId())
                .orElseThrow(ApplyFormNotFoundException::new);

        // 3. 활성화된 지원 폼의 ID를 사용해 지원자 페이지 조회
        return ApplicantPageServiceResponse.from(
                applicantRepository.findApplicantsPageWithSortCriteria(
                        request.sortCriteria(),
                        request.isEvaluating(),
                        request.cursor(),
                        request.size(),
                        mostRecentApplyForm.getId()
                ).toDto());
    }

    @Transactional(readOnly = true)
    public ApplicantDetailServiceResponse getApplicantDetail(String username, String applicantId) {
        // 1. 관리자 권한 검증
        Club club = clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);

        // 2. 지원자 ID로 지원자 정보 조회
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(ApplicantNotFoundException::new);

        // 3. 지원자의 동아리와 관리자의 동아리 비교
        validateApplicantAccess(applicant.getApplyForm().getClub().getId(), club.getId());

        // 4. 메모 정보 추출
        List<MemoResponse> memos = MemoResponse.fromList(applicant.getMemos());

        return ApplicantDetailServiceResponse.of(
                applicant.getName(),
                applicant.getAge(),
                applicant.getMajor(),
                applicant.getEmail(),
                applicant.getPhone(),
                applicant.getStudentStatus(),
                applicant.getGrade(),
                applicant.getGender(),
                applicant.getAnswers(),
                memos
        );
    }

    public ApplicantPageServiceResponse searchApplicantByKeyword(ApplicantSearchServiceRequest request) {
        // 1. 관리자 권한 검증
        Club club = clubRepository.findByAdminUsername(request.username())
                .orElseThrow(NotClubAdminException::new);

        // 2. 가장 최신의 지원 폼 찾기
        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(club.getId())
                .orElseThrow(ApplyFormNotFoundException::new);

        // 3. 지원자 검색
        return ApplicantPageServiceResponse.from(
                applicantRepository.searchApplicantsByKeyword(
                        request.searchKeyword(),
                        request.sortCriteria(),
                        request.isEvaluating(),
                        request.cursor(),
                        request.size(),
                        mostRecentApplyForm.getId()
                ).toDto());
    }

    @Transactional(readOnly = true)
    public ApplicantPageServiceResponse getApplicantsByStatus(ApplicantStatusServiceRequest request) {
        // 1. username으로 관리하는 동아리 찾기
        Club club = clubRepository.findByAdminUsername(request.username())
                .orElseThrow(NotClubAdminException::new);

        // 2. 가장 최신의 지원 폼 찾기
        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(club.getId())
                .orElseThrow(ApplyFormNotFoundException::new);

        // 3. 합격/불합격 상태에 따른 지원자 목록 조회
        return ApplicantPageServiceResponse.from(
                applicantRepository.findApplicantsByStatus(
                        request.isPassed(),
                        request.page(),
                        request.size(),
                        mostRecentApplyForm.getId()
                ).toDto());
    }

    @Transactional
    public void updateApplicantStatus(StatusUpdateServiceRequest request) {
        // 1. 관리자 권한 검증
        Club club = clubRepository.findByAdminUsername(request.username())
                .orElseThrow(NotClubAdminException::new);

        // 2. 지원자 ID로 지원자 정보 조회
        Applicant applicant = applicantRepository.findById(request.applicantId())
                .orElseThrow(ApplicantNotFoundException::new);

        // 3. 지원자의 동아리와 관리자의 동아리 비교
        validateApplicantAccess(applicant.getApplyForm().getClub().getId(), club.getId());

        // 4. 지원자 상태 업데이트
        applicant.updateStatus(request.status());
    }

    @Transactional
    public ApplicantFinalizeServiceResponse finalizeApplicantsStatus(ApplicantFinalizationRequest request) {
        // 1. 동아리 관리자 검증
        Club club = validateClubAdmin(request.username());

        // 2. 현재 활성화된 지원 폼 조회
        ApplyForm currentApplyForm = findActiveApplyForm(request.clubId());

        //TODO: 면접으로 추가 확장성 고려하기
        // 3. 현재 지원폼에 속한 지원자들 조회 및 합격자 처리
        int passedApplicantCount = processApplicants(currentApplyForm, club);

        // 4. 모든 합/불합격자 삭제 (평가 중 제외)
        int finalizedApplicantCount = applicantRepository.deleteAllApplicantsByApplyFormId(currentApplyForm.getId());

        return ApplicantFinalizeServiceResponse.of(passedApplicantCount, finalizedApplicantCount);
    }

    public void sendResultMailToApplicants(SendResultMailServiceRequest request,
                                           String username,
                                           String clubId) {
        // 1. 동아리 관리자 검증
        Club club = validateClubAdmin(username);

        // 2. 현재 활성화된 지원 폼 조회
        ApplyForm currentApplyForm = findActiveApplyForm(clubId);

        // 3. 지원자 목록 조회
        // 합격자 이메일 목록
        List<String> passedEmails = filterPassedApplicants(currentApplyForm.getId())
                .stream()
                .map(Applicant::getUserEmail)
                .toList();

        // 불합격자 이메일 목록
        List<String> failedEmails = applicantRepository.findByApplyFormId(currentApplyForm.getId())
                .stream()
                .filter(applicant -> applicant.getStatus() == Status.FAIL)
                .map(Applicant::getUserEmail)
                .toList();

        // 4. 이메일 전송
        emailService.sendResultMail(passedEmails, request.pass());
        emailService.sendResultMail(failedEmails, request.fail());
    }

    private Club validateClubAdmin(String username) {
        return clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);
    }

    private ApplyForm findActiveApplyForm(String clubId) {
        return applyFormRepository.findByClubIdAndStatus(clubId, ApplyFormStatus.ACTIVE)
                .orElseThrow(ActiveApplyFormNotFoundException::new);
    }

    // 합격 처리한 사용자 수 반환
    private int processApplicants(ApplyForm applyForm, Club club) {
        List<Applicant> passedApplicants = filterPassedApplicants(applyForm.getId());

        if (!passedApplicants.isEmpty()) {
            savePassedApplicantsAsClubMembers(passedApplicants, club);
        }

        return passedApplicants.size();
    }

    // 합격한 지원자들만 필터링하는 메서드
    private List<Applicant> filterPassedApplicants(String applyFormId) {
        return applicantRepository.findByApplyFormId(applyFormId)
                .stream()
                .filter(applicant -> applicant.getStatus() == Status.PASS)
                .toList();
    }

    // 합격 회원
    private void savePassedApplicantsAsClubMembers(List<Applicant> passedApplicants, Club club) {
        List<ClubMember> clubMembers = passedApplicants.stream()
                .map(passedApplicant -> convertToClubMember(passedApplicant, club))
                .toList();

        clubMemberRepository.saveAll(clubMembers);
    }

    private ClubMember convertToClubMember(Applicant applicant, Club club) {
        return ClubMember.builder()
                .club(club)
                .user(userRepository.findByEmail(applicant.getUserEmail())
                        .orElseThrow(ApplicantNotFoundException::new))
                .grade(applicant.getGrade())
                .major(applicant.getMajor())
                .build();
    }

    // 지원서가 현재 관리하는 동아리의 지원서인지 검증하는 메서드
    private void validateApplicantAccess(String applicantClubId, String targetClubId) {
        if (!applicantClubId.equals(targetClubId)) {
            throw new UnAuthorizedApplicantAccessException();
        }
    }
}
