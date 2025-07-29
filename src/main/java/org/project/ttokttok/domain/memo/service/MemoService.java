package org.project.ttokttok.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.BaseApplicant;
import org.project.ttokttok.domain.applicant.domain.DocumentApplicant;
import org.project.ttokttok.domain.applicant.exception.ApplicantNotFoundException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.memo.domain.Memo;
import org.project.ttokttok.domain.memo.exception.MemoNotFoundException;
import org.project.ttokttok.domain.memo.repository.MemoRepository;
import org.project.ttokttok.domain.memo.service.dto.request.CreateMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.DeleteMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.UpdateMemoServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemoService {

    //TODO: 올바른 관리자인지 검증 로직 추가 필요
    //private final MemoRepository memoRepository;
    private final ApplicantRepository applicantRepository;

    @Transactional
    public String createMemo(CreateMemoServiceRequest request) {
        BaseApplicant baseApplicant = getBaseApplicant(request.applicantId());

        if (!(baseApplicant instanceof DocumentApplicant documentApplicant))
            throw new IllegalArgumentException("메모는 서류 지원자에만 작성할 수 있습니다.");

        // 마지막에 추가된 메모의 ID 반환
        return documentApplicant.addMemo(request.content());
    }

    @Transactional
    public void updateMemo(UpdateMemoServiceRequest request) {
        BaseApplicant baseApplicant = getBaseApplicant(request.applicantId());

        if (!(baseApplicant instanceof DocumentApplicant documentApplicant))
            throw new IllegalArgumentException("메모는 서류 지원자에만 수정할 수 있습니다.");

        documentApplicant.updateMemo(request.memoId(), request.content());
    }

    @Transactional
    public void deleteMemo(DeleteMemoServiceRequest request) {
        BaseApplicant baseApplicant = getBaseApplicant(request.applicantId());

        if (!(baseApplicant instanceof DocumentApplicant documentApplicant))
            throw new IllegalArgumentException("메모는 서류 지원자에만 삭제할 수 있습니다.");

        documentApplicant.deleteMemo(request.memoId());
    }

    private BaseApplicant getBaseApplicant(String applicantId) {
        return applicantRepository.findById(applicantId)
                .orElseThrow(ApplicantNotFoundException::new);
    }
}
