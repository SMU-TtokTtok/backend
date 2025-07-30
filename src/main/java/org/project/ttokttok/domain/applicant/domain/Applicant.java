package org.project.ttokttok.domain.applicant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.enums.*;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.exception.InvalidPhaseTransitionException;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Table(name = "applicants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Applicant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    // 지원자 기본 정보 (변하지 않는 정보)
    private String userEmail;
    private String name;
    private Integer age;
    private String major;
    private String email;
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus studentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    // 현재 진행 단계
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicantPhase currentPhase;  // DOCUMENT_SUBMITTED, DOCUMENT_PASSED, INTERVIEW_SCHEDULED, INTERVIEW_COMPLETED, FINAL_PASSED, FINAL_FAILED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applyform_id")
    private ApplyForm applyForm;

    // 단계별 상세 정보 (1:1 관계, 필요시에만 존재)
    @OneToOne(mappedBy = "applicant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DocumentPhase documentPhase;

    @OneToOne(mappedBy = "applicant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private InterviewPhase interviewPhase;

    // 비즈니스 메서드
    public void submitDocument(List<Answer> answers) {
        // 초기 상태이거나, 재지원 가능한 상태인지 확인
        if (this.currentPhase != null && !canResubmitDocument()) {
            throw new InvalidPhaseTransitionException();
        }

        this.currentPhase = ApplicantPhase.DOCUMENT_SUBMITTED;
        this.documentPhase = DocumentPhase.create(this, answers);
    }

    public void passDocumentEvaluation() {
        validateCurrentPhase(ApplicantPhase.DOCUMENT_SUBMITTED);
        this.currentPhase = ApplicantPhase.DOCUMENT_PASSED;
        this.documentPhase.updateStatus(PhaseStatus.PASS);
    }

    public void failDocumentEvaluation() {
        validateCurrentPhase(ApplicantPhase.DOCUMENT_SUBMITTED);
        this.currentPhase = ApplicantPhase.FINAL_FAILED;
        this.documentPhase.updateStatus(PhaseStatus.FAIL);
    }

    public void scheduleInterview(LocalDate interviewDate) {
        validateCurrentPhase(ApplicantPhase.DOCUMENT_PASSED);
        this.currentPhase = ApplicantPhase.INTERVIEW_SCHEDULED;
        this.interviewPhase = InterviewPhase.create(this, interviewDate);
    }

    public void completeInterview(PhaseStatus result) {
        validateCurrentPhase(ApplicantPhase.INTERVIEW_SCHEDULED);
        this.currentPhase = ApplicantPhase.INTERVIEW_COMPLETED;
        this.interviewPhase.updateStatus(result);

        // 면접 결과에 따라 최종 단계 결정
        if (result == PhaseStatus.PASS) {
            this.currentPhase = ApplicantPhase.FINAL_PASSED;
        } else if (result == PhaseStatus.FAIL) {
            this.currentPhase = ApplicantPhase.FINAL_FAILED;
        }
    }

    // 단계 검증 메서드
    private void validateCurrentPhase(ApplicantPhase expectedPhase) {
        if (this.currentPhase != expectedPhase) {
            throw new InvalidPhaseTransitionException();
        }
    }

    // 단계 전환 가능성 체크 메서드들
    private boolean canResubmitDocument() {
        // 서류 재제출이 가능한 상태들
        return this.currentPhase == ApplicantPhase.FINAL_FAILED;
    }

    public boolean canScheduleInterview() {
        return this.currentPhase == ApplicantPhase.DOCUMENT_PASSED;
    }

    public boolean canEvaluateDocument() {
        return this.currentPhase == ApplicantPhase.DOCUMENT_SUBMITTED;
    }

    public boolean canCompleteInterview() {
        return this.currentPhase == ApplicantPhase.INTERVIEW_SCHEDULED;
    }

    // 편의 메서드들
    public boolean isInDocumentPhase() {
        return currentPhase == ApplicantPhase.DOCUMENT_SUBMITTED;
    }

    public boolean isInInterviewPhase() {
        return currentPhase == ApplicantPhase.INTERVIEW_SCHEDULED ||
                currentPhase == ApplicantPhase.INTERVIEW_COMPLETED;
    }

    public boolean hasInterviewPhase() {
        return interviewPhase != null;
    }
}
