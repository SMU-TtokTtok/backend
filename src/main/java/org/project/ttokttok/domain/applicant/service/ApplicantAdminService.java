package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.exception.ApplicantNotFoundException;
import org.project.ttokttok.domain.applicant.exception.UnAuthorizedApplicantAccessException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantPageServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantSearchServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantStatusServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.StatusUpdateServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantDetailServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantPageServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.MemoResponse;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicantAdminService {

    private final ApplicantRepository applicantRepository;
    private final ApplyFormRepository applyFormRepository;
    private final ClubRepository clubRepository;

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

    // 지원서가 현재 관리하는 동아리의 지원서인지 검증하는 메서드
    private void validateApplicantAccess(String applicantClubId, String targetClubId) {
        if (!applicantClubId.equals(targetClubId)) {
            throw new UnAuthorizedApplicantAccessException();
        }
    }
}
