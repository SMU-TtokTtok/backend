package org.project.ttokttok.domain.applyform.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.exception.ActiveApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.service.dto.response.ActiveApplyFormServiceResponse;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplyFormUserService {

    private final ApplyFormRepository applyFormRepository;
    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public ActiveApplyFormServiceResponse getActiveApplyForm(
            String userEmail,
            String clubId
    ) {
        validateClubExists(clubId);

        Map<String, Object> tempData = applyFormRepository.findTempData(userEmail, clubId);

        return applyFormRepository.findByClubIdAndStatus(clubId, ApplyFormStatus.ACTIVE)
                .map(applyForm -> ActiveApplyFormServiceResponse.of(
                        applyForm.getId(),
                        applyForm.getTitle(),
                        applyForm.getSubTitle(),
                        applyForm.getFormJson(),
                        tempData
                ))
                .orElseThrow(ActiveApplyFormNotFoundException::new);
    }

    private void validateClubExists(String clubId) {
        if (!clubRepository.existsById(clubId))
            throw new ClubNotFoundException();
    }
}
