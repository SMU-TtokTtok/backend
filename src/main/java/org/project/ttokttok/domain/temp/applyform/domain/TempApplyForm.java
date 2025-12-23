package org.project.ttokttok.domain.temp.applyform.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.global.entity.BaseTimeEntity;

@Entity
@Table(name = "temp_applyforms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TempApplyForm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Column(nullable = false, updatable = false)
    private String clubId;

    @Column(length = 100)
    private String title;

    private String subTitle;

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

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "temp_applyform_grades",
            joinColumns = @JoinColumn(name = "temp_applyform_id")
    )
    private Set<ApplicableGrade> grades = new HashSet<>(); // 지원 가능한 학년 목록

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Question> formJson = new ArrayList<>();

    @Builder
    private TempApplyForm(String clubId, String title, String subTitle,
                         LocalDate applyStartDate, LocalDate applyEndDate,
                         boolean hasInterview, LocalDate interviewStartDate,
                         LocalDate interviewEndDate, Integer maxApplyCount,
                         Set<ApplicableGrade> grades, List<Question> formJson) {
        this.clubId = clubId;
        this.title = title;
        this.subTitle = subTitle;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.hasInterview = hasInterview;
        this.interviewStartDate = interviewStartDate;
        this.interviewEndDate = interviewEndDate;
        this.maxApplyCount = maxApplyCount;
        this.grades = grades != null ? grades : new HashSet<>();
        this.formJson = formJson != null ? formJson : new ArrayList<>();
    }

    public static TempApplyForm create(String clubId, String title, String subTitle,
                                     LocalDate applyStartDate, LocalDate applyEndDate,
                                     boolean hasInterview, LocalDate interviewStartDate,
                                     LocalDate interviewEndDate, Integer maxApplyCount,
                                     Set<ApplicableGrade> grades, List<Question> formJson) {
        return TempApplyForm.builder()
                .clubId(clubId)
                .title(title)
                .subTitle(subTitle)
                .applyStartDate(applyStartDate)
                .applyEndDate(applyEndDate)
                .hasInterview(hasInterview)
                .interviewStartDate(interviewStartDate)
                .interviewEndDate(interviewEndDate)
                .maxApplyCount(maxApplyCount)
                .grades(grades)
                .formJson(formJson)
                .build();
    }

    /**
     * 임시 지원폼 정보를 업데이트합니다.
     */
    public void update(String title, String subTitle, LocalDate applyStartDate,
                      LocalDate applyEndDate, boolean hasInterview,
                      LocalDate interviewStartDate, LocalDate interviewEndDate,
                      Integer maxApplyCount, Set<ApplicableGrade> grades,
                      List<Question> formJson) {
        this.title = title;
        this.subTitle = subTitle;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.hasInterview = hasInterview;
        this.interviewStartDate = interviewStartDate;
        this.interviewEndDate = interviewEndDate;
        this.maxApplyCount = maxApplyCount;
        this.grades = grades != null ? grades : new HashSet<>();
        this.formJson = formJson != null ? formJson : new ArrayList<>();
    }
}