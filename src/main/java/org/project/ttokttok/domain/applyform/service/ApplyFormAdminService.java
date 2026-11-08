package org.project.ttokttok.domain.applyform.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;
import org.project.ttokttok.domain.applyform.exception.ApplyFormRuleException;
import org.project.ttokttok.domain.clubmember.repository.ClubMemberRepository;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.AlreadyActiveApplyFormExistsException;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.domain.ApplyFormInputPolicy;
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
    private final ApplicantRepository applicantRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final S3Service s3Service;

    @Transactional
    public String createApplyForm(ApplyFormCreateServiceRequest request) {
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        validateAdmin(club.getAdmin().getUsername(), request.username());

        ApplyFormInputPolicy.validateRecruitPeriod(request.recruitStartDate(), request.recruitEndDate());

        validateActiveFormExists(request.clubId());

        Set<ApplicableGrade> applicableGrades = request.applicableGrades()
                .stream()
                .map(ApplicableGrade::from)
                .collect(Collectors.toSet());
        validateFormContent(request, applicableGrades);

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
        ApplyForm applyForm = findOwnedForm(club, request.applyFormId());

        String title = request.title().isPresent() ? request.title().get() : null;
        String subtitle = request.subTitle().isPresent() ? request.subTitle().get() : null;
        List<Question> questions = request.questions().isPresent() ? request.questions().get() : null;

        if (questions != null && applyFormRepository.existsApplicantByFormId(applyForm.getId())) {
            applyForm.validateQuestionsEditableWithApplicants(questions);
        }

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
                            getBeforeForms(clubId),
                            false
                    ))
                    .orElse(ApplyFormDetailServiceResponse.of(
                            null, null, null, List.of(), getBeforeForms(clubId), false
                    ));
        }

        List<BeforeApplyFormServiceResponse> beforeForms = getBeforeForms(clubId);

        ApplyForm activeForm = applyForm.get();

        return ApplyFormDetailServiceResponse.of(
                activeForm.getId(),
                activeForm.getTitle(),
                activeForm.getSubTitle(),
                activeForm.getFormJson(),
                beforeForms,
                applyFormRepository.existsApplicantByFormId(activeForm.getId())
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

        return findOwnedForm(club, formId).getFormJson();
    }

    @Transactional
    public void finishEvaluation(String adminName, String formId) {
        Club club = validateClubAdmin(adminName);
        ApplyForm applyForm = findOwnedForm(club, formId);
        if (applyForm.isFinished()) {
            return;
        }
        if (applyForm.recruitState() == RecruitState.OPEN) {
            throw new ApplyFormRuleException(ErrorMessage.FINISH_WHILE_RECRUITING);
        }
        List<Applicant> applicants = applicantRepository.findByApplyFormId(formId);
        validateFinalPassersLinked(club, applicants);
        List<String> fileUrls = applicants.stream().flatMap(a -> a.fileAnswerUrls().stream()).toList();

        applyForm.finish();
        applyFormRepository.saveAndFlush(applyForm);

        applyFormRepository.deleteAllTempApplicantByFormId(formId);
        applyFormRepository.deleteAllApplicantByFormId(formId);
        fileUrls.forEach(s3Service::deleteFileAfterCommit);
    }

    /** 평가 중인 지원자는 막지 않는다. 부원으로 연동되지 않은 최종 합격자만 막는다. */
    private void validateFinalPassersLinked(Club club, List<Applicant> applicants) {
        long unlinked = applicants.stream()
                .filter(Applicant::isFinalPass)
                .filter(a -> !clubMemberRepository.existsByClubIdAndEmail(club.getId(), a.getEmail()))
                .count();
        if (unlinked > 0) {
            throw new ApplyFormRuleException(ErrorMessage.FINISH_UNLINKED_PASSERS,
                    "부원으로 연동되지 않은 최종 합격자가 " + unlinked + "명 있습니다. '최종 부원연동'을 먼저 눌러 주세요.");
        }
    }

    private Club validateClubAdmin(String username) {
        return clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);
    }

    private ApplyForm findOwnedForm(Club club, String formId) {
        ApplyForm applyForm = applyFormRepository.findById(formId)
                .orElseThrow(ApplyFormNotFoundException::new);

        if (!applyForm.getClub().getId().equals(club.getId())) {
            throw new NotClubAdminException();
        }
        return applyForm;
    }

    private void validateAdmin(String adminName, String requestAdminName) {
        if (!adminName.equals(requestAdminName)) {
            throw new NotClubAdminException();
        }
    }

    private void validateFormContent(ApplyFormCreateServiceRequest request, Set<ApplicableGrade> grades) {
        ApplyFormInputPolicy.validateGrades(grades);
        ApplyFormInputPolicy.validateQuestions(request.questions());
        ApplyFormInputPolicy.validateMaxApplyCount(request.maxApplyCount());
        ApplyFormInputPolicy.validateInterview(request.hasInterview(),
                request.interviewStartDate(), request.interviewEndDate());
    }
}
