package org.project.ttokttok.domain.temp.applyform.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.temp.applyform.controller.dto.request.TempApplyFormSaveRequest;
import org.project.ttokttok.domain.temp.applyform.domain.TempApplyForm;
import org.project.ttokttok.domain.temp.applyform.repository.TempApplyFormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TempApplyFormService {

    private final TempApplyFormRepository tempApplyFormRepository;

    /**
     * 임시 지원폼을 저장합니다.
     * 기존 임시 지원폼이 있다면 업데이트하고, 없다면 새로 생성합니다.
     */
    public String saveTempApplyForm(TempApplyFormSaveRequest request) {
        // 기존 임시 지원폼이 있는지 확인
        Optional<TempApplyForm> existingTempApplyForm =
            tempApplyFormRepository.findByClubId(request.clubId());

        if (existingTempApplyForm.isPresent()) {
            // 기존 임시 지원폼 업데이트
            TempApplyForm tempApplyForm = existingTempApplyForm.get();
            updateTempApplyForm(tempApplyForm, request);
            return tempApplyForm.getId();
        } else {
            // 새로운 임시 지원폼 생성
            TempApplyForm newTempApplyForm = TempApplyForm.create(
                    request.clubId(),
                    request.title(),
                    request.subTitle(),
                    request.applyStartDate(),
                    request.applyEndDate(),
                    request.hasInterview() != null ? request.hasInterview() : false,
                    request.interviewStartDate(),
                    request.interviewEndDate(),
                    request.maxApplyCount(),
                    request.grades(),
                    request.formJson()
            );

            TempApplyForm saved = tempApplyFormRepository.save(newTempApplyForm);
            return saved.getId();
        }
    }

    /**
     * 특정 동아리의 임시 지원폼을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Optional<TempApplyForm> getTempApplyForm(String clubId) {
        return tempApplyFormRepository.findByClubId(clubId);
    }

    /**
     * 모든 임시 지원폼을 조회합니다. (관리자용)
     */
    @Transactional(readOnly = true)
    public List<TempApplyForm> getAllTempApplyForms() {
        return tempApplyFormRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 임시 지원폼을 삭제합니다.
     */
    public void deleteTempApplyForm(String clubId) {
        Optional<TempApplyForm> tempApplyForm =
            tempApplyFormRepository.findByClubId(clubId);

        tempApplyForm.ifPresent(tempApplyFormRepository::delete);
    }

    /**
     * 임시 지원폼 존재 여부를 확인합니다.
     */
    @Transactional(readOnly = true)
    public boolean existsTempApplyForm(String clubId) {
        return tempApplyFormRepository.existsByClubId(clubId);
    }

    private void updateTempApplyForm(TempApplyForm tempApplyForm,
                                   TempApplyFormSaveRequest request) {
        tempApplyForm.update(
                request.title(),
                request.subTitle(),
                request.applyStartDate(),
                request.applyEndDate(),
                request.hasInterview() != null ? request.hasInterview() : false,
                request.interviewStartDate(),
                request.interviewEndDate(),
                request.maxApplyCount(),
                request.grades(),
                request.formJson()
        );
    }
}
