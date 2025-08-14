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
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ApplicantPageServiceResponse getApplicantPage(ApplicantPageServiceRequest request) {
        // 1. username으로 관리하는 동아리 찾기
        Club club = clubRepository.findByAdminUsername(request.username())
                .orElseThrow(NotClubAdminException::new);

        // 2. 가장 최신의 지원 폼 찾기
        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(club.getId(), ACTIVE)
                .orElse(null);

        if (mostRecentApplyForm == null) {
            return ApplicantPageServiceResponse.toEmpty();
        }

        // 3. 활성화된 지원 폼의 ID를 사용해 지원자 페이지 조회
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
        // 1. 관리자 권한 검증
        Club club = clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);

        // 2. 지원자 조회 (DocumentPhase와 함께)
        Applicant applicant = applicantRepository.findByIdWithDocumentPhase(applicantId)
                .orElseThrow(ApplicantNotFoundException::new);

        // 3. 지원자의 동아리와 관리자의 동아리 비교
        validateApplicantAccess(applicant.getApplyForm().getClub().getId(), club.getId());

        // 4. 메모 정보 추출 (DocumentPhase에서)
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
                // answers도 DocumentPhase에서 가져오기
                applicant.getDocumentPhase() != null ?
                        applicant.getDocumentPhase().getAnswers() : Collections.emptyList(),
                memos
        );
    }

    public ApplicantPageServiceResponse searchApplicantByKeyword(ApplicantSearchServiceRequest request) {
        // 1. 관리자 권한 검증
        Club club = clubRepository.findByAdminUsername(request.username())
                .orElseThrow(NotClubAdminException::new);

        // 2. 가장 최신의 지원 폼 찾기
        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(club.getId(), ACTIVE)
                .orElse(null);

        if (mostRecentApplyForm == null) {
            return ApplicantPageServiceResponse.toEmpty();
        }

        // 3. 지원자 검색
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

    // todo: 비어있으면 null 혹은 빈 배열 반환
    @Transactional(readOnly = true)
    public ApplicantPageServiceResponse getApplicantsByStatus(ApplicantStatusServiceRequest request) {
        // 1. username으로 관리하는 동아리 찾기
        Club club = clubRepository.findByAdminUsername(request.username())
                .orElseThrow(NotClubAdminException::new);

        // 2. 가장 최신의 지원 폼 찾기
        ApplyForm mostRecentApplyForm = applyFormRepository.findTopByClubIdAndStatusOrderByCreatedAtDesc(club.getId(), ACTIVE)
                .orElse(null);

        if (mostRecentApplyForm == null) {
            return ApplicantPageServiceResponse.toEmpty();
        }

        // 3. 합격/불합격 상태에 따른 지원자 목록 조회
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

    // ok
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
        updateApplicantPhaseStatus(applicant, request.status(), request.kind());
    }

    @Transactional
    public ApplicantFinalizeServiceResponse finalizeApplicantsStatus(ApplicantFinalizationRequest request) {
        // 1. 동아리 관리자 검증
        Club club = validateClubAdmin(request.username());

        // 2. 현재 활성화된 지원 폼 조회
        ApplyForm currentApplyForm = findActiveApplyForm(request.clubId());

        // 3. 분기 boolean 값 설정
        boolean isDocument = Kind.isDocument(request.kind());

        // 4. 현재 지원폼에 속한 지원자들 조회 및 합격자 처리
        int passedApplicantCount = processApplicants(currentApplyForm, club, isDocument);

        // 5. 최종 확정된 지원자 수 계산 (합격자 + 불합격자, 평가 중 제외)
        int finalizedApplicantCount = calculateFinalizedApplicantCount(currentApplyForm.getId(), isDocument) + passedApplicantCount;

        return ApplicantFinalizeServiceResponse.of(passedApplicantCount, finalizedApplicantCount);
    }

    @Transactional
    public void sendResultMailToApplicants(SendResultMailServiceRequest request,
                                           String username,
                                           String clubId,
                                           String kind) {
        // 1. 동아리 관리자 검증
        validateClubAdmin(username);

        // 2. 현재 활성화된 지원 폼 조회
        ApplyForm currentApplyForm = findActiveApplyForm(clubId);

        // 서류 또는 면접 단계인지 확인
        boolean isDocument = Kind.isDocument(kind);

        // 3. 지원자 목록 조회
        // 합격자 이메일 목록
        List<String> passedEmails = filterApplicantsByStatus(currentApplyForm.getId(), isDocument, PASS)
                .stream()
                .map(Applicant::getEmail)
                .toList();

        // 불합격자 이메일 목록
        List<String> failedEmails = filterApplicantsByStatus(currentApplyForm.getId(), isDocument, FAIL)
                .stream()
                .map(Applicant::getEmail)
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
        return applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE)
                .orElseThrow(ActiveApplyFormNotFoundException::new);
    }

    // 합격 처리한 사용자 수 반환
    private int processApplicants(ApplyForm applyForm, Club club, boolean isDocument) {
        // 서류 단계면, 지원자와 연동된 interviewPhase 생성하고 저장.
        // 면접 단계면, 최종적으로 ClubMember 엔티티 생성하고 저장
        List<Applicant> passedApplicants = filterApplicantsByStatus(applyForm.getId(), isDocument, PASS);

        if (!passedApplicants.isEmpty() && !isDocument) {
            // 면접 단계인 경우, ClubMember로 변환하여 저장
            savePassedApplicantsAsClubMembers(passedApplicants, club);
        } else if (!passedApplicants.isEmpty() && applyForm.isHasInterview()) {
            passedApplicants.stream()
                    // 면접 단계가 아닌 사람만
                    .filter(applicant -> !applicant.isInInterviewPhase())
                    .forEach(applicant ->
                            applicant.updateToInterviewPhase(applyForm.getInterviewStartDate())
                    );
        } else if (!passedApplicants.isEmpty()) {
            // 서류 단계인 경우, 합격 상태로 업데이트
            savePassedApplicantsAsClubMembers(passedApplicants, club);
        }
        return passedApplicants.size();
    }

    // 최종 확정된 지원자 수 계산 ->
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

    // 상태에 따라 지원자 목록을 필터링하는 메서드
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

    // 합격한 지원자들을 ClubMember로 변환하여 저장하는 메서드
    private void savePassedApplicantsAsClubMembers(List<Applicant> passedApplicants, Club club) {

        List<Applicant> validApplicants = passedApplicants.stream()
                .filter(applicant -> {
                    // 이미 ClubMember로 등록된 지원자 제외
                    boolean alreadyMember = clubMemberRepository
                            .existsByClubIdAndUserEmail(club.getId(), applicant.getUserEmail());
                    if (alreadyMember) {
                        log.warn("지원자 {}는 이미 동아리 부원으로 등록되어 있습니다.", applicant.getUserEmail());
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
                userRepository.findByEmail(applicant.getUserEmail())
                        .orElseThrow(ApplicantNotFoundException::new),
                MEMBER,
                applicant.getGrade(),
                applicant.getMajor(),
                applicant.getUserEmail(),
                applicant.getPhone(),
                applicant.getGender()
        );
    }

    // 지원서가 현재 관리하는 동아리의 지원서인지 검증하는 메서드
    private void validateApplicantAccess(String applicantClubId, String targetClubId) {
        if (!applicantClubId.equals(targetClubId)) {
            throw new UnAuthorizedApplicantAccessException();
        }
    }

    // 지원자의 상태를 업데이트하는 메서드
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
