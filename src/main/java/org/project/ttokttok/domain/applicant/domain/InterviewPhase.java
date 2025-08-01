package org.project.ttokttok.domain.applicant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "interview_phases")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewPhase extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @OneToOne
    @JoinColumn(name = "applicant_id")
    private Applicant applicant;

    private LocalDate interviewDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseStatus status;  // PASS, FAIL, EVALUATING

    public static InterviewPhase create(Applicant applicant, LocalDate interviewDate) {
        InterviewPhase phase = new InterviewPhase();
        phase.applicant = applicant;
        phase.interviewDate = interviewDate;
        phase.status = PhaseStatus.EVALUATING;

        return phase;
    }

    public void updateStatus(PhaseStatus status) {
        this.status = status;
    }

    public void updateInterviewDate(LocalDate interviewDate) {
        this.interviewDate = interviewDate;
    }
}

