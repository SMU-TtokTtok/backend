package org.project.ttokttok.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.exception.ApplicantNotFoundException;
import org.project.ttokttok.domain.applicant.exception.UnAuthorizedApplicantAccessException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.memo.service.dto.request.CreateMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.DeleteMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.UpdateMemoServiceRequest;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemoService {

    private final ApplicantRepository applicantRepository;
    private final ClubRepository clubRepository;

    @Transactional
    public String createMemo(String username, CreateMemoServiceRequest request) {
        Club club = validateClubAdmin(username);
        Applicant applicant = getApplicant(request.applicantId());

        // 해당 관리자의 동아리 지원자인지 검증
        validateApplicantClub(applicant, club.getId());

        if (applicant.getDocumentPhase() == null)
            throw new IllegalArgumentException("메모는 서류 지원자에만 작성할 수 있습니다.");

        return applicant.getDocumentPhase().addMemo(request.content());
    }

    @Transactional
    public void updateMemo(String username, UpdateMemoServiceRequest request) {
        Club club = validateClubAdmin(username);
        Applicant applicant = getApplicant(request.applicantId());

        // 해당 관리자의 동아리 지원자인지 검증
        validateApplicantClub(applicant, club.getId());

        if (applicant.getDocumentPhase() == null)
            throw new IllegalArgumentException("메모는 서류 지원자에만 수정할 수 있습니다.");

        applicant.getDocumentPhase().updateMemo(request.memoId(), request.content());
    }

    @Transactional
    public void deleteMemo(String username, DeleteMemoServiceRequest request) {
        Club club = validateClubAdmin(username);
        Applicant applicant = getApplicant(request.applicantId());

        // 해당 관리자의 동아리 지원자인지 검증
        validateApplicantClub(applicant, club.getId());

        if (applicant.getDocumentPhase() == null)
            throw new IllegalArgumentException("메모는 서류 지원자에만 삭제할 수 있습니다.");

        applicant.getDocumentPhase().deleteMemo(request.memoId());
    }

    private Applicant getApplicant(String applicantId) {
        return applicantRepository.findById(applicantId)
                .orElseThrow(ApplicantNotFoundException::new);
    }

    private void validateApplicantClub(Applicant applicant, String clubId) {
        if (!applicant.getApplyForm().getClub().getId().equals(clubId)) {
            throw new UnAuthorizedApplicantAccessException();
        }
    }

    private Club validateClubAdmin(String username) {
        return clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);
    }
}
