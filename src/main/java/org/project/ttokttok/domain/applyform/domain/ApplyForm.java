package org.project.ttokttok.domain.applyform.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
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
    private LocalDateTime applyStartDate;

    @Column(nullable = false)
    private LocalDateTime applyDeadline;

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

    // 추후 JsonNode나 Map으로 개선
    @Column(columnDefinition = "JSONB", nullable = false)
    private String formJson;

    public void updateApplyInfo(LocalDateTime applyStartDate,
                                LocalDateTime applyDeadline,
                                Integer maxApplyCount,
                                Set<ApplicableGrade> grades,
                                Boolean isRecruiting) {

        this.applyStartDate = applyStartDate != null ? applyStartDate : this.applyStartDate;
        this.applyDeadline = applyDeadline != null ? applyDeadline : this.applyDeadline;
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
