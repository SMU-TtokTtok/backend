package org.project.ttokttok.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.Applicant;
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
        Applicant applicant = applicantRepository.findById(request.applicantId())
                .orElseThrow(ApplicantNotFoundException::new);

        // 마지막에 추가된 메모의 ID 반환
        return applicant.addMemo(request.content());
    }

    @Transactional
    public void updateMemo(UpdateMemoServiceRequest request) {
        Applicant applicant = applicantRepository.findById(request.applicantId())
                .orElseThrow(ApplicantNotFoundException::new);

        // 편의 메서드 활용
        applicant.updateMemo(request.memoId(), request.content());
    }

    @Transactional
    public void deleteMemo(DeleteMemoServiceRequest request) {
        Applicant applicant = applicantRepository.findById(request.applicantId())
                .orElseThrow(ApplicantNotFoundException::new);

        // 편의 메서드 활용
        applicant.deleteMemo(request.memoId());
    }
}
