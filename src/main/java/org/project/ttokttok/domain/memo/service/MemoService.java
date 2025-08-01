package org.project.ttokttok.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.exception.ApplicantNotFoundException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.memo.service.dto.request.CreateMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.DeleteMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.UpdateMemoServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemoService {

    //TODO: 올바른 관리자인지 검증 로직 추가 필요
    private final ApplicantRepository applicantRepository;

    @Transactional
    public String createMemo(CreateMemoServiceRequest request) {
        Applicant applicant = getApplicant(request.applicantId());

        if (applicant.getDocumentPhase() == null)
            throw new IllegalArgumentException("메모는 서류 지원자에만 작성할 수 있습니다.");

        return applicant.getDocumentPhase().addMemo(request.content());
    }

    @Transactional
    public void updateMemo(UpdateMemoServiceRequest request) {
        Applicant applicant = getApplicant(request.applicantId());

        if (applicant.getDocumentPhase() == null)
            throw new IllegalArgumentException("메모는 서류 지원자에만 수정할 수 있습니다.");

        applicant.getDocumentPhase().updateMemo(request.memoId(), request.content());
    }

    @Transactional
    public void deleteMemo(DeleteMemoServiceRequest request) {
        Applicant applicant = getApplicant(request.applicantId());

        if (applicant.getDocumentPhase() == null)
            throw new IllegalArgumentException("메모는 서류 지원자에만 삭제할 수 있습니다.");

        applicant.getDocumentPhase().deleteMemo(request.memoId());
    }

    private Applicant getApplicant(String applicantId) {
        return applicantRepository.findById(applicantId)
                .orElseThrow(ApplicantNotFoundException::new);
    }
}
