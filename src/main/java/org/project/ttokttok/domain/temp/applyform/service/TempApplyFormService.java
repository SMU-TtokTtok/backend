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
public class TempApplyFormService {

    private final TempApplyFormRepository tempApplyFormRepository;

    /**
     * 임시 지원폼을 저장합니다. 기존 임시 지원폼이 있다면 업데이트하고, 없다면 새로 생성합니다.
     */
    @Transactional
    public String saveTempApplyForm(TempApplyFormSaveRequest request) {

        // 기존 임시 지원폼이 있는지 확인하고 있으면 업데이트
        String tempApplyFormId = tempApplyFormRepository.findByClubId(request.clubId())
                .map(temp -> {
                    updateTempApplyForm(temp, request);
                    return temp.getId();
                })
                .orElseGet(() -> { // 없으면 저장
                    return tempApplyFormRepository.save(
                            createTempApplyForm(request)).getId();
                });

        return tempApplyFormId;
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

    private TempApplyForm createTempApplyForm(TempApplyFormSaveRequest request) {
        return TempApplyForm.create(
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
    }
}
