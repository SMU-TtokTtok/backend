package org.project.ttokttok.domain.applyform.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
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
        this.status = ApplyFormStatus.ACTIVE;
        this.formJson = formJson;
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

    //TODO: Mapper를 통해서 수정하도록 변경
    public void updateApplyInfo(LocalDate applyStartDate,
                                LocalDate applyDeadline,
                                Integer maxApplyCount,
                                Set<ApplicableGrade> grades,
                                Boolean isRecruiting) {

        this.applyStartDate = applyStartDate != null ? applyStartDate : this.applyStartDate;
        this.applyEndDate = applyDeadline != null ? applyDeadline : this.applyEndDate;
        this.maxApplyCount = maxApplyCount != null ? maxApplyCount : this.maxApplyCount;
        if (grades != null) {
            this.grades.clear();
            this.grades.addAll(grades);
        }

        if (isRecruiting != null) {
            this.status = isRecruiting ? ApplyFormStatus.ACTIVE : ApplyFormStatus.INACTIVE;
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

    // FIXME: 모집일자의 예외 처리가 필요합니다.
    // EX: DEADLINE이 지난 경우, 자동으로 모집 마감 처리 등
    public void updateFormStatus() {
        if (this.status == ApplyFormStatus.ACTIVE) {
            this.status = ApplyFormStatus.INACTIVE;
        } else if (this.status == ApplyFormStatus.INACTIVE) {
            this.status = ApplyFormStatus.ACTIVE;
        }
    }
}
