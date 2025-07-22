package org.project.ttokttok.domain.applyform.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.exception.ActiveApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.service.dto.response.ActiveApplyFormServiceResponse;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplyFormUserService {

    private final ApplyFormRepository applyFormRepository;
    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public ActiveApplyFormServiceResponse getActiveApplyForm(String clubId) {
        validateClubExists(clubId);

        return applyFormRepository.findByClubIdAndStatus(clubId, ApplyFormStatus.ACTIVE)
                .map(applyForm -> ActiveApplyFormServiceResponse.of(
                        applyForm.getTitle(),
                        applyForm.getSubTitle(),
                        applyForm.getFormJson()
                ))
                .orElseThrow(ActiveApplyFormNotFoundException::new);
    }

    private void validateClubExists(String clubId) {
        if (!clubRepository.existsById(clubId))
            throw new ClubNotFoundException();
    }
}
