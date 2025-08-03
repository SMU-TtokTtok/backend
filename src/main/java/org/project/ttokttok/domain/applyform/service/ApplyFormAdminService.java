package org.project.ttokttok.domain.applyform.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.json.Question;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

@Service
@RequiredArgsConstructor
public class ApplyFormAdminService {

    private final ApplyFormRepository applyFormRepository;
    private final ClubRepository clubRepository;

    // 지원 폼 생성 메서드
    @Transactional
    public String createApplyForm(ApplyFormCreateServiceRequest request) {
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        // 관리자 권한 검증
        validateAdmin(club.getAdmin().getUsername(), request.username());

        // 날짜 범위 검증
        validateDateRange(request.recruitStartDate(), request.recruitEndDate());

        // 숫자 입력으로 들어온 set을 ApplicableGrade로 변환
        Set<ApplicableGrade> applicableGrades = request.applicableGrades()
                .stream()
                .map(ApplicableGrade::from)
                .collect(Collectors.toSet());

        // 지원 폼 생성
        ApplyForm applyForm = ApplyForm.createApplyForm(
                club,
                request.hasInterview(),
                request.recruitStartDate(),
                request.recruitEndDate(),
                request.interviewStartDate().orElse(null),
                request.interviewEndDate().orElse(null),
                request.maxApplyCount(),
                applicableGrades,
                request.title(),
                request.subTitle(),
                request.questions()
        );

        return applyFormRepository.save(applyForm)
                .getId();
    }

    // 지원 폼 수정 메서드
    // todo: 이미 지원한 사용자가 있는 경우, 수정 불가능하도록 예외 처리 필요.
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

        // 활성화된 지원 폼 조회
        ApplyForm applyForm = applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE)
                .orElseThrow(ApplyFormNotFoundException::new);

        // 이전에 사용했던 질문 목록 리스트 조회
        List<BeforeApplyFormServiceResponse> beforeForms = applyFormRepository.findByClubId(clubId)
                .stream()
                .filter(form -> form.getStatus() == ACTIVE)
                .map(form -> BeforeApplyFormServiceResponse.of(
                        form.getId(),
                        LocalDate.from(form.getCreatedAt())
                ))
                .toList();

        return ApplyFormDetailServiceResponse.of(
                applyForm.getId(),
                applyForm.getTitle(),
                applyForm.getSubTitle(),
                applyForm.getFormJson(),
                beforeForms
        );
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
