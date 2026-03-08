package org.project.ttokttok.domain.applyform.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.AlreadyActiveApplyFormExistsException;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.exception.InvalidDateRangeException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormCreateServiceRequest;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormUpdateServiceRequest;
import org.project.ttokttok.domain.applyform.service.dto.response.ApplyFormDetailServiceResponse;
import org.project.ttokttok.domain.applyform.service.dto.response.BeforeApplyFormServiceResponse;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.temp.applyform.repository.TempApplyFormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

@Service
@RequiredArgsConstructor
public class ApplyFormAdminService {

    private final ApplyFormRepository applyFormRepository;
    private final ClubRepository clubRepository;
    private final TempApplyFormRepository tempApplyFormRepository;

    @Transactional
    public String createApplyForm(ApplyFormCreateServiceRequest request) {
        Club club = validateClubAdmin(request.username());

        validateDateRange(request.recruitStartDate(), request.recruitEndDate());

        validateActiveFormExists(request.clubId());

        Set<ApplicableGrade> applicableGrades = request.applicableGrades()
                .stream()
                .map(ApplicableGrade::from)
                .collect(Collectors.toSet());

        tempApplyFormRepository.findByClubId(club.getId())
                .ifPresent(tempApplyFormRepository::delete);

        ApplyForm applyForm = ApplyForm.createApplyForm(
                club,
                request.hasInterview(),
                request.recruitStartDate(),
                request.recruitEndDate(),
                request.interviewStartDate(),
                request.interviewEndDate(),
                request.maxApplyCount(),
                applicableGrades,
                request.title(),
                request.subTitle(),
                request.questions()
        );

        return applyFormRepository.save(applyForm)
                .getId();
    }

    private void validateActiveFormExists(String clubId) {
        if (applyFormRepository.existsByClubIdAndStatus(clubId, ACTIVE)) {
            throw new AlreadyActiveApplyFormExistsException();
        }
    }

    @Transactional
    public void updateApplyForm(ApplyFormUpdateServiceRequest request) {
        Club club = validateClubAdmin(request.username());

        ApplyForm applyForm = applyFormRepository.findById(request.applyFormId())
                .orElseThrow(ApplyFormNotFoundException::new);

        validateAdmin(club.getAdmin().getUsername(), request.username());

        String title = request.title().isPresent() ? request.title().get() : null;
        String subtitle = request.subTitle().isPresent() ? request.subTitle().get() : null;
        List<Question> questions = request.questions().isPresent() ? request.questions().get() : null;

        applyForm.updateFormContent(title, subtitle, questions);
    }

    @Transactional(readOnly = true)
    public ApplyFormDetailServiceResponse getApplyFormDetail(String username, String clubId) {
        validateClubAdmin(username);

        Optional<ApplyForm> applyForm = applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE);

        if (applyForm.isEmpty()) {
            return tempApplyFormRepository.findByClubId(clubId)
                    .map(tempForm -> ApplyFormDetailServiceResponse.of(
                            tempForm.getId(),
                            tempForm.getTitle(),
                            tempForm.getSubTitle(),
                            tempForm.getFormJson(),
                            getBeforeForms(clubId)
                    ))
                    .orElse(ApplyFormDetailServiceResponse.of(
                            null, null, null, List.of(), getBeforeForms(clubId)
                    ));
        }

        List<BeforeApplyFormServiceResponse> beforeForms = getBeforeForms(clubId);

        return ApplyFormDetailServiceResponse.of(
                applyForm.map(ApplyForm::getId).orElse(null),
                applyForm.map(ApplyForm::getTitle).orElse(null),
                applyForm.map(ApplyForm::getSubTitle).orElse(null),
                applyForm.map(ApplyForm::getFormJson).orElse(List.of()),
                beforeForms
        );
    }

    private List<BeforeApplyFormServiceResponse> getBeforeForms(String clubId) {
        return applyFormRepository.findByClubId(clubId)
                .stream()
                .filter(form -> form.getStatus() != ACTIVE)
                .map(form -> BeforeApplyFormServiceResponse.of(
                        form.getId(),
                        LocalDate.from(form.getCreatedAt())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Question> getPreviousApplyFormQuestions(String username, String formId) {
        Club club = validateClubAdmin(username);
        
        ApplyForm applyForm = applyFormRepository.findById(formId)
                .orElseThrow(ApplyFormNotFoundException::new);

        if (!applyForm.getClub().getId().equals(club.getId())) {
            throw new NotClubAdminException();
        }

        return applyForm.getFormJson();
    }

    @Transactional
    public void finishEvaluation(String adminName, String formId) {
        Club club = validateClubAdmin(adminName);
        
        ApplyForm applyForm = applyFormRepository.findById(formId)
                .orElseThrow(ApplyFormNotFoundException::new);

        if (!applyForm.getClub().getId().equals(club.getId())) {
            throw new NotClubAdminException();
        }

        applyForm.updateFormStatus();
        applyFormRepository.saveAndFlush(applyForm);

        applyFormRepository.deleteAllTempApplicantByFormId(formId);
        applyFormRepository.deleteAllApplicantByFormId(formId);
    }

    private Club validateClubAdmin(String username) {
        return clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);
    }

    private void validateAdmin(String adminName, String requestAdminName) {
        if (!adminName.equals(requestAdminName)) {
            throw new NotClubAdminException();
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException();
        }
    }
}
