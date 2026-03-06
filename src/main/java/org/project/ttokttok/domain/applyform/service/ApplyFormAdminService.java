package org.project.ttokttok.domain.applyform.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
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
import org.project.ttokttok.global.annotation.auth.RequireClubAdmin;
import org.project.ttokttok.global.auth.ClubHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
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

    // 지원 폼 생성 메서드
    @Transactional
    @RequireClubAdmin
    public String createApplyForm(ApplyFormCreateServiceRequest request) {
        // 1. AOP에서 주입된 동아리 정보 가져오기
        Club club = ClubHolder.getClub();

        // 2. 날짜 범위 검증
        validateDateRange(request.recruitStartDate(), request.recruitEndDate());

        // 3. 활성화된 폼이 존재하는지 파악.
        validateActiveFormExists(request.clubId());

        // 4. 숫자 입력으로 들어온 set을 ApplicableGrade로 변환
        Set<ApplicableGrade> applicableGrades = request.applicableGrades()
                .stream()
                .map(ApplicableGrade::from)
                .collect(Collectors.toSet());

        // 5. 임시저장한 지원폼이 존재하면 삭제
        tempApplyFormRepository.findByClubId(club.getId())
                .ifPresent(tempApplyFormRepository::delete);

        // 6. 지원 폼 생성
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

    // 지원 폼 수정 메서드
    @Transactional
    @RequireClubAdmin
    public void updateApplyForm(ApplyFormUpdateServiceRequest request) {
        // 1. AOP에서 주입된 동아리 정보 가져오기
        Club club = ClubHolder.getClub();

        ApplyForm applyForm = applyFormRepository.findById(request.applyFormId())
                .orElseThrow(ApplyFormNotFoundException::new);

        // 2. 관리자 권한 검증 (AOP에서 이미 동아리 소유권 검증됨)

        // 3. JsonNullable 값 추출
        String title = request.title().isPresent() ? request.title().get() : null;
        String subtitle = request.subTitle().isPresent() ? request.subTitle().get() : null;
        List<Question> questions = request.questions().isPresent() ? request.questions().get() : null;

        // 4. 지원 폼 수정
        applyForm.updateFormContent(title, subtitle, questions);
    }

    // 동아리의 지원 폼 목록 조회 메서드
    @Transactional(readOnly = true)
    @RequireClubAdmin
    public ApplyFormDetailServiceResponse getApplyFormDetail(String username, String clubId) {
        // 1. AOP에서 주입된 동아리 정보 가져오기
        Club club = ClubHolder.getClub();

        // 2. 활성화된 지원 폼 조회 -> 없을 경우 임시 지원폼을 조회하는 방식으로 변경.
        Optional<ApplyForm> applyForm = applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE);

        // 3. 활성화된 지원 폼이 존재하지 않는다면, 
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

        // 4. 이전에 사용했던 질문 목록 리스트 조회
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

    // 이전에 사용한 지원폼의 질문 조회 메서드 추가
    @Transactional(readOnly = true)
    @RequireClubAdmin
    public List<Question> getPreviousApplyFormQuestions(String username, String formId) {
        ApplyForm applyForm = applyFormRepository.findById(formId)
                .orElseThrow(ApplyFormNotFoundException::new);

        // AOP를 통해 이미 해당 관리자의 동아리임이 검증됨 (validateAdmin 대신 AOP 사용)
        // 단, formId가 해당 동아리의 것인지 추가 확인 필요
        if (!applyForm.getClub().getId().equals(ClubHolder.getClub().getId())) {
            throw new NotClubAdminException();
        }

        // 질문 목록 반환
        return applyForm.getFormJson();
    }

    // 이번 분기 지원을 종료
    @Transactional
    @RequireClubAdmin
    public void finishEvaluation(String adminName, String formId) {
        ApplyForm applyForm = applyFormRepository.findById(formId)
                .orElseThrow(ApplyFormNotFoundException::new);

        // AOP를 통해 이미 해당 관리자의 동아리임이 검증됨
        if (!applyForm.getClub().getId().equals(ClubHolder.getClub().getId())) {
            throw new NotClubAdminException();
        }

        // 분기 모집 종료 시 ->
        // 1. 지원 폼 비활성화
        applyForm.updateFormStatus();
        applyFormRepository.saveAndFlush(applyForm);

        // 2. 모든 임시 지원자 데이터 삭제
        applyFormRepository.deleteAllTempApplicantByFormId(formId);

        // 3. 모든 지원자들에 대한 정보 삭제 (DocumentPhase, InterviewPhase는 CASCADE로 자동 삭제)
        // DB 레벨에도 CASCADE 옵션이 걸려있는 상태.
        applyFormRepository.deleteAllApplicantByFormId(formId);
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
