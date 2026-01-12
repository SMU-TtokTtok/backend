package org.project.ttokttok.domain.temp.applicant.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempApplicantSaveRequest;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TempApplicantService {

    private final TempApplicantRepository tempApplicantRepository;

    /**
     * 임시 지원서를 저장합니다. 기존 임시 지원서가 있다면 업데이트하고, 없다면 새로 생성합니다.
     */
    public String saveTempApplicant(String userEmail, TempApplicantSaveRequest request) {
        Optional<TempApplicant> optionalTempApplicant =
                tempApplicantRepository.findByUserEmailAndFormId(userEmail, request.formId());

        if (optionalTempApplicant.isPresent()) {
            TempApplicant tempApplicant = optionalTempApplicant.get();
            tempApplicant.update(request.tempData());

            return tempApplicant.getId();
        }

        TempApplicant createdApplicant = TempApplicant.create(
                request.formId(),
                userEmail,
                request.tempData()
        );

        return tempApplicantRepository.save(createdApplicant)
                .getId();
    }
}
