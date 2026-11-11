package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.project.ttokttok.domain.club.service.policy.ClubAccessPolicy;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.exception.ActiveApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubmember.domain.ClubMember;
import org.project.ttokttok.domain.clubmember.repository.ClubMemberRepository;
import org.project.ttokttok.domain.notification.mail.service.ResultMailDeliveryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus.FAIL;
import static org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus.PASS;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.clubmember.domain.MemberRole.MEMBER;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantAdminService {

    private final ApplicantRepository applicantRepository;
    private final ApplyFormRepository applyFormRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ResultMailDeliveryService resultMailDeliveryService;
    private final ClubAccessPolicy clubAccessPolicy;

    public ApplicantPageServiceResponse getApplicantPage(ApplicantPageServiceRequest request) {
        Club club = validateClubAdmin(request.username());

        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(club.getId(),
                        ACTIVE)
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

        if (!applicant.belongsToClub(club.getId())) {
            throw new UnAuthorizedApplicantAccessException();
        }

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

        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(club.getId(),
                        ACTIVE)
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

        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(club.getId(),
                        ACTIVE)
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

        if (!applicant.belongsToClub(club.getId())) {
            throw new UnAuthorizedApplicantAccessException();
        }

        ApplicantPhase phase = request.kind();
        applicant.changeEvaluationStatus(phase, request.status());
    }

    @Transactional
    public ApplicantFinalizeServiceResponse finalizeApplicantsStatus(ApplicantFinalizationRequest request) {
        // 같은 동아리의 확정을 직렬화한다. 부원 존재 확인과 저장 사이에 다른 요청이 끼어들어
        // 같은 지원자가 중복 등록되는 것을 막고, 반복·동시 요청이 같은 결과를 받게 한다. (#432)
        Club club = clubRepository.findByIdForUpdate(request.clubId())
                .orElseThrow(ClubNotFoundException::new);
        clubAccessPolicy.validateAdmin(club, request.username());

        ApplyForm currentApplyForm = findActiveApplyForm(request.clubId());
        ApplicantPhase phase = request.kind();
        int passedApplicantCount = processApplicants(currentApplyForm, club, phase);
        int finalizedApplicantCount =
                calculateFinalizedApplicantCount(currentApplyForm.getId(), phase) + passedApplicantCount;

        return ApplicantFinalizeServiceResponse.of(passedApplicantCount, finalizedApplicantCount);
    }

    // 커밋이 곧 발송 작업 수락이다. 발송은 워커가 트랜잭션 밖에서 한다(#460).
    @Transactional
    public void sendResultMailToApplicants(SendResultMailServiceRequest request,
                                           String username,
                                           String clubId,
                                           ApplicantPhase kind) {

        ResultMailRecipients recipients = findResultMailRecipients(username, clubId, kind);

        String requestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        resultMailDeliveryService.enqueue(requestId, clubId, recipients.passed(), request.pass(), now);
        resultMailDeliveryService.enqueue(requestId, clubId, recipients.failed(), request.fail(), now);
    }

    // 관리자 확인과 전형 상태 조회가 지연 로딩을 쓰므로 이메일 문자열까지 트랜잭션 안에서 꺼낸다.
    private ResultMailRecipients findResultMailRecipients(String username, String clubId, ApplicantPhase phase) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(ClubNotFoundException::new);

        clubAccessPolicy.validateAdmin(club, username);

        ApplyForm currentApplyForm = findActiveApplyForm(clubId);

        List<String> passedEmails = filterApplicantsByStatus(currentApplyForm.getId(), phase, PASS)
                .stream()
                .map(Applicant::getEmail)
                .toList();

        List<String> failedEmails = filterApplicantsByStatus(currentApplyForm.getId(), phase, FAIL)
                .stream()
                .map(Applicant::getEmail)
                .toList();

        List<String> passed = passedEmails.stream().filter(this::hasEmail).toList();
        List<String> failed = failedEmails.stream().filter(this::hasEmail).toList();
        int skipped = passedEmails.size() + failedEmails.size() - passed.size() - failed.size();
        if (skipped > 0) {
            log.warn("결과 메일 수신자 중 이메일이 없는 지원자를 건너뜀: {}명", skipped);
        }

        return new ResultMailRecipients(passed, failed);
    }

    private boolean hasEmail(String email) {
        return email != null && !email.isBlank();
    }

    private record ResultMailRecipients(List<String> passed, List<String> failed) {
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
                        log.debug("지원자 {}는 이미 동아리 부원으로 등록되어 있습니다.", applicant.getId());
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

}
