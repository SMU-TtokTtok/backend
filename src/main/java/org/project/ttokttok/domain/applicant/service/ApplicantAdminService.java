package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.ResultAnnouncement;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.exception.*;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.repository.ResultAnnouncementRepository;
import org.project.ttokttok.domain.applicant.service.dto.request.*;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantDetailServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantFinalizeServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantPageServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.MemoResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ResultAnnouncementServiceResponse;
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
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    private final ResultAnnouncementRepository resultAnnouncementRepository;
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

    // 확정과 발표 기록, 수신자별 발송 작업을 한 커밋으로 저장한다. 커밋이 곧 발송 작업 수락이다(#465).
    // 동아리 행 잠금으로 같은 동아리의 발표·확정 요청을 줄 세운다. UNIQUE 제약(V33)은 마지막 방어선이다.
    @Transactional
    public ResultAnnouncementServiceResponse announceResults(ResultAnnouncementServiceRequest request) {
        Club club = clubRepository.findByIdForUpdate(request.clubId())
                .orElseThrow(ClubNotFoundException::new);
        clubAccessPolicy.validateAdmin(club, request.username());

        String fingerprint = request.fingerprint();
        Optional<ResultAnnouncement> previous =
                resultAnnouncementRepository.findByClubIdAndIdempotencyKey(club.getId(), request.idempotencyKey());
        if (previous.isPresent()) {
            return ResultAnnouncementServiceResponse.from(previous.get().replay(fingerprint));
        }
        return ResultAnnouncementServiceResponse.from(announce(club, request, fingerprint));
    }

    private ResultAnnouncement announce(Club club, ResultAnnouncementServiceRequest request, String fingerprint) {
        ApplyForm form = findActiveApplyForm(club.getId());
        ApplicantPhase phase = request.kind();
        if (resultAnnouncementRepository.existsByApplyFormIdAndPhase(form.getId(), phase)) {
            throw new ResultAnnouncementException(ErrorMessage.RESULT_ALREADY_ANNOUNCED);
        }
        processApplicants(form, club, phase);

        ResultMailRecipients recipients = collectResultMailRecipients(form.getId(), phase);
        SendResultMailServiceRequest mail = request.mail();
        ResultAnnouncement announcement = resultAnnouncementRepository.save(ResultAnnouncement.of(
                club.getId(), form.getId(), phase, request.idempotencyKey(), fingerprint,
                mailCount(recipients.passed(), mail.pass()), mailCount(recipients.failed(), mail.fail())));
        enqueueResultMails(announcement.getId(), club.getId(), recipients, mail);
        return announcement;
    }

    private int mailCount(List<String> recipients, ResultMailContent content) {
        return content == null ? 0 : recipients.size();
    }

    // 커밋이 곧 발송 작업 수락이다. 발송은 워커가 트랜잭션 밖에서 한다(#460).
    @Transactional
    public void sendResultMailToApplicants(SendResultMailServiceRequest request,
                                           String username,
                                           String clubId,
                                           ApplicantPhase kind) {

        ResultMailRecipients recipients = findResultMailRecipients(username, clubId, kind);
        enqueueResultMails(UUID.randomUUID().toString(), clubId, recipients, request);
    }

    private void enqueueResultMails(String requestId, String clubId, ResultMailRecipients recipients,
                                    SendResultMailServiceRequest mail) {
        LocalDateTime now = LocalDateTime.now();
        resultMailDeliveryService.enqueue(requestId, clubId, recipients.passed(), mail.pass(), now);
        resultMailDeliveryService.enqueue(requestId, clubId, recipients.failed(), mail.fail(), now);
    }

    private ResultMailRecipients findResultMailRecipients(String username, String clubId, ApplicantPhase phase) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(ClubNotFoundException::new);

        clubAccessPolicy.validateAdmin(club, username);

        return collectResultMailRecipients(findActiveApplyForm(clubId).getId(), phase);
    }

    // 전형 상태 조회가 지연 로딩을 쓰므로 이메일 문자열까지 트랜잭션 안에서 꺼낸다.
    private ResultMailRecipients collectResultMailRecipients(String applyFormId, ApplicantPhase phase) {
        List<String> passedEmails = filterApplicantsByStatus(applyFormId, phase, PASS)
                .stream()
                .map(Applicant::getEmail)
                .toList();

        List<String> failedEmails = filterApplicantsByStatus(applyFormId, phase, FAIL)
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
