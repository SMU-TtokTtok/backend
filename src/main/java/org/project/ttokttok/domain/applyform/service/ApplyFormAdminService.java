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
    public String createApplyForm(ApplyFormCreateServiceRequest request) {
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        // 관리자 권한 검증
        validateAdmin(club.getAdmin().getUsername(), request.username());

        // 날짜 범위 검증
        validateDateRange(request.recruitStartDate(), request.recruitEndDate());

        // 활성화된 폼이 존재하는지 파악.
        validateActiveFormExists(request.clubId());

        // 숫자 입력으로 들어온 set을 ApplicableGrade로 변환
        Set<ApplicableGrade> applicableGrades = request.applicableGrades()
                .stream()
                .map(ApplicableGrade::from)
                .collect(Collectors.toSet());

        // 임시저장한 지원폼이 존재하면 삭제
        tempApplyFormRepository.findByClubId(club.getId())
                .ifPresent(tempApplyFormRepository::delete);

        // 지원 폼 생성
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
    public void updateApplyForm(ApplyFormUpdateServiceRequest request) {
        ApplyForm applyForm = applyFormRepository.findById(request.applyFormId())
                .orElseThrow(ApplyFormNotFoundException::new);

        // 관리자 권한 검증
        validateAdmin(applyForm.getClub().getAdmin().getUsername(), request.username());

        // JsonNullable 값 추출
        String title = request.title().isPresent() ? request.title().get() : null;
        String subtitle = request.subtitle().isPresent() ? request.subtitle().get() : null;
        List<Question> questions = request.questions().isPresent() ? request.questions().get() : null;

        // 지원 폼 수정
        applyForm.updateFormContent(title, subtitle, questions);
    }

    // 동아리의 지원 폼 목록 조회 메서드
    @Transactional(readOnly = true)
    public ApplyFormDetailServiceResponse getApplyFormDetail(String username, String clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(ClubNotFoundException::new);

        // 관리자 권한 검증
        validateAdmin(club.getAdmin().getUsername(), username);

        // 활성화된 지원 폼 조회 -> 없을 경우 임시 지원폼을 조회하는 방식으로 변경.
        Optional<ApplyForm> applyForm = applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE);

        // 활성화된 지원 폼이 존재하지 않는다면, 
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

        // 이전에 사용했던 질문 목록 리스트 조회
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
    public List<Question> getPreviousApplyFormQuestions(String username, String formId) {
        ApplyForm applyForm = applyFormRepository.findById(formId)
                .orElseThrow(ApplyFormNotFoundException::new);

        // 관리자 권한 검증
        validateAdmin(applyForm.getClub().getAdmin().getUsername(), username);

        // 질문 목록 반환
        return applyForm.getFormJson();
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
