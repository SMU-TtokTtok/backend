package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.admin.exception.AdminNotFoundException;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.exception.ApplicantNotFoundException;
import org.project.ttokttok.domain.applicant.exception.UnauthorizedApplicantAccessException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantPageServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantDetailServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantPageServiceResponse;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

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

        // 2. 현재 활성화된 지원 폼 찾기
        ApplyForm activeApplyForm = applyFormRepository.findTopByClubIdOrderByCreatedAtDesc(club.getId())
                .orElseThrow(ApplyFormNotFoundException::new);

        // 3. 활성화된 지원 폼의 ID를 사용해 지원자 페이지 조회
        return ApplicantPageServiceResponse.from(applicantRepository.findApplicantsPageWithSortCriteria(
                request.sortCriteria(),
                request.isEvaluating(),
                request.cursor(),
                request.size(),
                activeApplyForm.getId()
        ).toDto());
    }

    // 폼 관리 여부 검증 로직 추가 필요
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

        return ApplicantDetailServiceResponse.of(
                applicant.getName(),
                applicant.getAge(),
                applicant.getMajor(),
                applicant.getEmail(),
                applicant.getPhone(),
                applicant.getStudentStatus(),
                applicant.getGrade(),
                applicant.getGender(),
                applicant.getAnswers()
        );
    }

    private void validateApplicantAccess(String applicantClubId, String targetClubId) {
        if (!applicantClubId.equals(targetClubId)) {
            // 지원서가 현재 관리하는 동아리의 지원서가 아닐 경우 예외 발생
            throw new UnauthorizedApplicantAccessException();
        }
    }
}
