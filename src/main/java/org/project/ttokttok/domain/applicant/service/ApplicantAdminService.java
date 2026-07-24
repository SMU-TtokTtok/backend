package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.controller.enums.Kind;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.exception.*;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.service.dto.request.*;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantDetailServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantFinalizeServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantPageServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.MemoResponse;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.exception.ActiveApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus.FAIL;
import static org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus.PASS;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.clubMember.domain.MemberRole.MEMBER;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantAdminService {

    private final ApplicantRepository applicantRepository;
    private final ApplyFormRepository applyFormRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final EmailService emailService;

    public ApplicantPageServiceResponse getApplicantPage(ApplicantPageServiceRequest request) {
        Club club = validateClubAdmin(request.username());

        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(club.getId(), ACTIVE)
                .orElse(null);

        if (mostRecentApplyForm == null) {
            return ApplicantPageServiceResponse.toEmpty();
        }

        return ApplicantPageServiceResponse.of(
                applicantRepository.findApplicantsPageWithSortCriteria(
                        request.sortCriteria(),
                        request.isEvaluating(),
                        request.cursor(),
                        request.size(),
                        mostRecentApplyForm.getId(),
                        request.kind()
                ).toDto(),
                mostRecentApplyForm.isHasInterview());
    }

    @Transactional(readOnly = true)
    public ApplicantDetailServiceResponse getApplicantDetail(String username, String applicantId) {
        Club club = validateClubAdmin(username);

        Applicant applicant = applicantRepository.findByIdWithDocumentPhase(applicantId)
                .orElseThrow(ApplicantNotFoundException::new);

        validateApplicantAccess(applicant.getApplyForm().getClub().getId(), club.getId());

        List<MemoResponse> memos = new ArrayList<>();
        if (applicant.getDocumentPhase() != null) {
            memos = MemoResponse.fromList(applicant.getDocumentPhase().getMemos());
        }

        return ApplicantDetailServiceResponse.of(
                applicant.getName(),
                applicant.getAge(),
                applicant.getMajor(),
                applicant.getEmail(),
                applicant.getPhone(),
                applicant.getStudentStatus(),
                applicant.getGrade(),
                applicant.getGender(),
                applicant.getDocumentPhase() != null ?
                        applicant.getDocumentPhase().getAnswers() : Collections.emptyList(),
                memos
        );
    }

    public ApplicantPageServiceResponse searchApplicantByKeyword(ApplicantSearchServiceRequest request) {
        Club club = validateClubAdmin(request.username());

        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(club.getId(), ACTIVE)
                .orElse(null);

        if (mostRecentApplyForm == null) {
            return ApplicantPageServiceResponse.toEmpty();
        }

        return ApplicantPageServiceResponse.of(
                applicantRepository.searchApplicantsByKeyword(
                        request.searchKeyword(),
                        request.sortCriteria(),
                        request.isEvaluating(),
                        request.cursor(),
                        request.size(),
                        mostRecentApplyForm.getId(),
                        request.kind()
                ).toDto(),
                mostRecentApplyForm.isHasInterview());
    }

    @Transactional(readOnly = true)
    public ApplicantPageServiceResponse getApplicantsByStatus(ApplicantStatusServiceRequest request) {
        Club club = validateClubAdmin(request.username());

        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(club.getId(), ACTIVE)
                .orElse(null);

        if (mostRecentApplyForm == null) {
            return ApplicantPageServiceResponse.toEmpty();
        }

        return ApplicantPageServiceResponse.of(
                applicantRepository.findApplicantsByStatus(
                        request.isPassed(),
                        request.page(),
                        request.size(),
                        mostRecentApplyForm.getId(),
                        request.kind()
                ).toDto(),
                mostRecentApplyForm.isHasInterview());
    }

    @Transactional
    public void updateApplicantStatus(StatusUpdateServiceRequest request) {
        Club club = validateClubAdmin(request.username());

        Applicant applicant = applicantRepository.findById(request.applicantId())
                .orElseThrow(ApplicantNotFoundException::new);

        validateApplicantAccess(applicant.getApplyForm().getClub().getId(), club.getId());

        ApplicantPhase phase = Kind.toApplicantPhase(request.kind());
        applicant.changeEvaluationStatus(phase, request.status());
    }

    @Transactional
    public ApplicantFinalizeServiceResponse finalizeApplicantsStatus(ApplicantFinalizationRequest request) {
        Club club = validateClubAdmin(request.username());

        ApplyForm currentApplyForm = findActiveApplyForm(request.clubId());
        ApplicantPhase phase = Kind.toApplicantPhase(request.kind());
        int passedApplicantCount = processApplicants(currentApplyForm, club, phase);
        int finalizedApplicantCount = calculateFinalizedApplicantCount(currentApplyForm.getId(), phase) + passedApplicantCount;

        return ApplicantFinalizeServiceResponse.of(passedApplicantCount, finalizedApplicantCount);
    }

    // 조회 전용(쓰기 없음). 발송은 EmailService의 @Async로 트랜잭션 밖에서 처리되어
    // SMTP I/O가 커넥션 점유 시간을 늘리지 않는다. readOnly로 lazy loading 세션만 유지한다.
    @Transactional(readOnly = true)
    public void sendResultMailToApplicants(SendResultMailServiceRequest request,
                                           String username,
                                           String clubId,
                                           String kind) {
        validateClubAdmin(username);
        
        ApplyForm currentApplyForm = findActiveApplyForm(clubId);
        ApplicantPhase phase = Kind.toApplicantPhase(kind);

        List<String> passedEmails = filterApplicantsByStatus(currentApplyForm.getId(), phase, PASS)
                .stream()
                .map(Applicant::getEmail)
                .toList();

        List<String> failedEmails = filterApplicantsByStatus(currentApplyForm.getId(), phase, FAIL)
                .stream()
                .map(Applicant::getEmail)
                .toList();

        emailService.sendResultMail(passedEmails, request.pass());
        emailService.sendResultMail(failedEmails, request.fail());
    }

    private Club validateClubAdmin(String username) {
        return clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);
    }

    private ApplyForm findActiveApplyForm(String clubId) {
        return applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE)
                .orElseThrow(ActiveApplyFormNotFoundException::new);
    }

    private int processApplicants(ApplyForm applyForm, Club club, ApplicantPhase phase) {
        List<Applicant> passedApplicants = filterApplicantsByStatus(applyForm.getId(), phase, PASS);

        if (!passedApplicants.isEmpty() && phase == ApplicantPhase.INTERVIEW) {
            savePassedApplicantsAsClubMembers(passedApplicants, club);
        } else if (!passedApplicants.isEmpty() && applyForm.isHasInterview()) {
            passedApplicants.stream()
                    .filter(applicant -> !applicant.isInInterviewPhase())
                    .forEach(applicant ->
                            applicant.updateToInterviewPhase(applyForm.getInterviewStartDate())
                    );
        } else if (!passedApplicants.isEmpty()) {
            savePassedApplicantsAsClubMembers(passedApplicants, club);
        }
        return passedApplicants.size();
    }

    private int calculateFinalizedApplicantCount(String applyFormId, ApplicantPhase phase) {
        return (int) applicantRepository.findByApplyFormId(applyFormId)
                .stream()
                .filter(applicant -> applicant.statusOf(phase)
                        .filter(status -> status == FAIL)
                        .isPresent())
                .count();
    }

    private List<Applicant> filterApplicantsByStatus(String applyFormId, ApplicantPhase phase, PhaseStatus status) {
        return applicantRepository.findByApplyFormId(applyFormId)
                .stream()
                .filter(applicant -> applicant.statusOf(phase)
                        .filter(phaseStatus -> phaseStatus == status)
                        .isPresent())
                .toList();
    }

    private void savePassedApplicantsAsClubMembers(List<Applicant> passedApplicants, Club club) {

        List<Applicant> validApplicants = passedApplicants.stream()
                .filter(applicant -> {
                    boolean alreadyMember = clubMemberRepository
                            .existsByClubIdAndEmail(club.getId(), applicant.getEmail());
                    if (alreadyMember) {
                        log.warn("지원자 {}는 이미 동아리 부원으로 등록되어 있습니다.", applicant.getEmail());
                        return false;
                    }
                    return true;
                })
                .toList();

        List<ClubMember> clubMembers = validApplicants.stream()
                .map(passedApplicant -> convertToClubMember(passedApplicant, club))
                .toList();

        if (!clubMembers.isEmpty()) {
            clubMemberRepository.saveAll(clubMembers);
        }
    }

    private ClubMember convertToClubMember(Applicant applicant, Club club) {
        return ClubMember.create(
                club,
                applicant.getName(),
                MEMBER,
                applicant.getGrade(),
                applicant.getMajor(),
                applicant.getEmail(),
                applicant.getPhone(),
                applicant.getGender()
        );
    }

    private void validateApplicantAccess(String applicantClubId, String targetClubId) {
        if (!applicantClubId.equals(targetClubId)) {
            throw new UnAuthorizedApplicantAccessException();
        }
    }
}
