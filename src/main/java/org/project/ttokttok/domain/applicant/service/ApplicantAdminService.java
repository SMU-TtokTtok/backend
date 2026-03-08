package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.controller.enums.Kind;
import org.project.ttokttok.domain.applicant.domain.Applicant;
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

        updateApplicantPhaseStatus(applicant, request.status(), request.kind());
    }

    @Transactional
    public ApplicantFinalizeServiceResponse finalizeApplicantsStatus(ApplicantFinalizationRequest request) {
        Club club = validateClubAdmin(request.username());

        ApplyForm currentApplyForm = findActiveApplyForm(request.clubId());
        boolean isDocument = Kind.isDocument(request.kind());
        int passedApplicantCount = processApplicants(currentApplyForm, club, isDocument);
        int finalizedApplicantCount = calculateFinalizedApplicantCount(currentApplyForm.getId(), isDocument) + passedApplicantCount;

        return ApplicantFinalizeServiceResponse.of(passedApplicantCount, finalizedApplicantCount);
    }

    @Transactional
    public void sendResultMailToApplicants(SendResultMailServiceRequest request,
                                           String username,
                                           String clubId,
                                           String kind) {
        validateClubAdmin(username);
        
        ApplyForm currentApplyForm = findActiveApplyForm(clubId);
        boolean isDocument = Kind.isDocument(kind);

        List<String> passedEmails = filterApplicantsByStatus(currentApplyForm.getId(), isDocument, PASS)
                .stream()
                .map(Applicant::getEmail)
                .toList();

        List<String> failedEmails = filterApplicantsByStatus(currentApplyForm.getId(), isDocument, FAIL)
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

    private int processApplicants(ApplyForm applyForm, Club club, boolean isDocument) {
        List<Applicant> passedApplicants = filterApplicantsByStatus(applyForm.getId(), isDocument, PASS);

        if (!passedApplicants.isEmpty() && !isDocument) {
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

    private int calculateFinalizedApplicantCount(String applyFormId, boolean isDocument) {
        return applicantRepository.findByApplyFormId(applyFormId)
                .stream()
                .mapToInt(applicant -> {
                    Integer status = failApplicantCount(isDocument, applicant);
                    if (status != null)
                        return status;
                    return 0;
                })
                .sum();
    }

    private Integer failApplicantCount(boolean isDocument, Applicant applicant) {
        if (isDocument && applicant.isInDocumentPhase()) {
            PhaseStatus status = applicant.getDocumentPhase() != null ?
                    applicant.getDocumentPhase().getStatus() : null;
            return (status == FAIL) ? 1 : 0;
        } else if (!isDocument && applicant.isInInterviewPhase()) {
            PhaseStatus status = applicant.hasInterviewPhase() ?
                    applicant.getInterviewPhase().getStatus() : null;
            return (status == FAIL) ? 1 : 0;
        }
        return null;
    }

    private List<Applicant> filterApplicantsByStatus(String applyFormId, boolean isDocument, PhaseStatus status) {
        return applicantRepository.findByApplyFormId(applyFormId)
                .stream()
                .filter(applicant -> {
                    if (isDocument) {
                        return applicant.isInDocumentPhase() && applicant.getDocumentPhase().getStatus() == status;
                    } else if (applicant.isInInterviewPhase()) {
                        return applicant.hasInterviewPhase() && applicant.getInterviewPhase().getStatus() == status;
                    }
                    return false;
                })
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

    private void updateApplicantPhaseStatus(Applicant applicant, PhaseStatus status, String kind) {

        boolean isDocument = Kind.isDocument(kind);

        if (status == PASS) {
            handlePassStatus(applicant, isDocument);
        } else if (status == PhaseStatus.FAIL) {
            handleFailStatus(applicant, isDocument);
        } else if (status == PhaseStatus.EVALUATING) {
            handleEvaluatingStatus(applicant, isDocument);
        }
    }

    private void handlePassStatus(Applicant applicant, boolean isDocument) {
        if (isDocument) {
            applicant.passDocumentEvaluation();
        } else {
            applicant.passInterview();
        }
    }

    private void handleFailStatus(Applicant applicant, boolean isDocument) {
        if (isDocument) {
            applicant.failDocumentEvaluation();
        } else {
            applicant.failInterview();
        }
    }

    private void handleEvaluatingStatus(Applicant applicant, boolean isDocument) {
        if (isDocument) {
            applicant.setDocumentEvaluating();
        } else {
            applicant.setInterviewEvaluating();
        }
    }
}
