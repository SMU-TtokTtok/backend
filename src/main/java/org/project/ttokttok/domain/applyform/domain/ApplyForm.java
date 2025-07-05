package org.project.ttokttok.domain.applyform.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Table(name = "applyforms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplyForm extends BaseTimeEntity {

    @Id
    @Column(length = 36, updatable = false, unique = true)
    private String id = UUID.randomUUID().toString();

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
    private Integer maxApplyCount;

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

//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(columnDefinition = "jsonb", nullable = false)
//    private List<Question> formJson;

    @Builder
    private ApplyForm(Club club,
                      LocalDate applyStartDate,
                      LocalDate applyEndDate,
                      int maxApplyCount,
                      Set<ApplicableGrade> grades,
                      String title,
                      String subTitle) {
        this.club = club;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.maxApplyCount = maxApplyCount;
        if (grades != null) {
            this.grades = new HashSet<>(grades);
        }
        this.title = title;
        this.subTitle = subTitle;
        this.status = ApplyFormStatus.ACTIVE;
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
}


