package org.project.ttokttok.domain.applyform.domain;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.*;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.ApplyFormRuleException;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.time.LocalDate;
import java.util.*;

@Entity
@Getter
@Table(name = "applyforms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplyForm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Column(nullable = false, length = 100)
    private String title;

    private String subTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplyFormStatus status; // 지원서 상태

    @Column(nullable = false)
    private LocalDate applyStartDate;

    @Column(nullable = false)
    private LocalDate applyEndDate;

    @Column(nullable = false)
    private boolean hasInterview; // 면접 전형 존재 여부

    private LocalDate interviewStartDate;

    private LocalDate interviewEndDate;

    @Column(nullable = false)
    private Integer maxApplyCount;

    @Column(nullable = false)
    private boolean isRecruiting;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "applyform_grades",
            joinColumns = @JoinColumn(name = "applyform_id")
    )
    @Column(nullable = false)
    private Set<ApplicableGrade> grades = new HashSet<>(); // 지원 가능한 학년 목록

    @JoinColumn(name = "club_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Club club;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Question> formJson = new ArrayList<>();

    @Builder
    private ApplyForm(Club club,
                      boolean hasInterview,
                      LocalDate applyStartDate,
                      LocalDate applyEndDate,
                      LocalDate interviewStartDate,
                      LocalDate interviewEndDate,
                      int maxApplyCount,
                      Set<ApplicableGrade> grades,
                      String title,
                      String subTitle,
                      List<Question> formJson) {
        this.club = club;
        this.hasInterview = hasInterview;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.interviewStartDate = interviewStartDate;
        this.interviewEndDate = interviewEndDate;
        this.maxApplyCount = maxApplyCount;
        this.grades = grades != null ? grades : new HashSet<>();
        this.title = title;
        this.subTitle = subTitle;
        this.status = ACTIVE;
        this.formJson = formJson;
        this.isRecruiting = true; // 기본값은 true로 설정
    }

    public static ApplyForm createApplyForm(Club club,
                                            boolean hasInterview,
                                            LocalDate applyStartDate,
                                            LocalDate applyEndDate,
                                            LocalDate interviewStartDate,
                                            LocalDate interviewEndDate,
                                            int maxApplyCount,
                                            Set<ApplicableGrade> grades,
                                            String title,
                                            String subTitle,
                                            List<Question> formJson) {
        return ApplyForm.builder()
                .club(club)
                .hasInterview(hasInterview)
                .applyStartDate(applyStartDate)
                .applyEndDate(applyEndDate)
                .interviewStartDate(interviewStartDate)
                .interviewEndDate(interviewEndDate)
                .maxApplyCount(maxApplyCount)
                .grades(grades)
                .title(title)
                .subTitle(subTitle)
                .formJson(formJson)
                .build();
    }

    public void updateApplyInfo(LocalDate applyStartDate,
                                LocalDate applyDeadline,
                                Integer maxApplyCount,
                                Set<ApplicableGrade> grades) {
        LocalDate nextStart = applyStartDate != null ? applyStartDate : this.applyStartDate;
        LocalDate nextEnd = applyDeadline != null ? applyDeadline : this.applyEndDate;
        ApplyFormInputPolicy.validateRecruitPeriod(nextStart, nextEnd);
        if (maxApplyCount != null) {
            ApplyFormInputPolicy.validateMaxApplyCount(maxApplyCount);
        }
        if (grades != null) {
            ApplyFormInputPolicy.validateGrades(grades);
        }

        this.applyStartDate = nextStart;
        this.applyEndDate = nextEnd;
        this.maxApplyCount = maxApplyCount != null ? maxApplyCount : this.maxApplyCount;
        if (grades != null) {
            this.grades.clear();
            this.grades.addAll(grades);
        }
    }

    public void updateFormContent(String title, String subTitle, List<Question> formJson) {
        if (title != null) {
            this.title = title;
        }
        if (subTitle != null) {
            this.subTitle = subTitle;
        }
        if (formJson != null) {
            this.formJson = formJson;
        }
    }

    /** 평가 종료. 이미 종료된 폼이어도 종료 상태로만 남는다. */
    public void finish() {
        this.status = INACTIVE;
    }

    public boolean isFinished() {
        return this.status == INACTIVE;
    }

    /**
     * 지원서가 제출된 폼에 새 질문 목록을 적용해도 되는지 검증한다.
     * 규칙은 {@link ApplyFormEditLockPolicy} 에 정의되어 있다.
     */
    public void validateQuestionsEditableWithApplicants(List<Question> updatedQuestions) {
        ApplyFormEditLockPolicy.validate(this.formJson, updatedQuestions);
    }

    public void updateFormStatus() {
        this.status = (this.status == ACTIVE)
                ? INACTIVE
                : ACTIVE;
    }

    public void toggleRecruiting() {
        this.isRecruiting = !this.isRecruiting;
    }

    /** 모집 시작. 오늘(한국 시간)이 모집 기간 안일 때만 열 수 있다. */
    public void startRecruiting() {
        LocalDate today = RecruitStatePolicy.today();
        if (today.isBefore(applyStartDate) || today.isAfter(applyEndDate)) {
            throw new ApplyFormRuleException(ErrorMessage.RECRUIT_PERIOD_NOT_OPEN);
        }
        this.isRecruiting = true;
    }

    /** 종료된 폼을 되살린다. 지난 기간으로 바로 열리지 않도록 모집 마감 상태로 시작한다. */
    public void reactivate() {
        this.status = ACTIVE;
        this.isRecruiting = false;
    }

    /** 지원서가 제출된 폼은 시작일을 바꿀 수 없다. */
    public void validateStartDateChange(LocalDate newStartDate, boolean hasApplicants) {
        if (hasApplicants && newStartDate != null && !newStartDate.equals(applyStartDate)) {
            throw new ApplyFormRuleException(ErrorMessage.APPLY_START_DATE_LOCKED);
        }
    }


    public void endRecruiting() {
        this.isRecruiting = false;
    }

    /** 오늘(한국 시간) 기준 모집 상태. 판정 규칙은 {@link RecruitStatePolicy} 에 있다. */
    public RecruitState recruitState() {
        return recruitState(RecruitStatePolicy.today());
    }

    public RecruitState recruitState(LocalDate today) {
        return RecruitStatePolicy.of(status, isRecruiting, applyStartDate, applyEndDate, today);
    }

    /** 지원서 제출은 모집 중일 때만 받는다. */
    public void validateOpenForApply() {
        RecruitState state = recruitState();
        if (state == RecruitState.UPCOMING) {
            throw new ApplyFormRuleException(ErrorMessage.APPLY_FORM_NOT_STARTED);
        }
        if (state != RecruitState.OPEN) {
            throw new ApplyFormRuleException(ErrorMessage.APPLY_FORM_CLOSED);
        }
    }

    /**
     * 지원 마감이 임박했는지 여부
     *
     * <p>판정 규칙은 {@link ApplyDeadlinePolicy} 에 정의되어 있다.
     */
    public boolean isDeadlineImminent() {
        return ApplyDeadlinePolicy.isImminent(this.applyEndDate);
    }
}
