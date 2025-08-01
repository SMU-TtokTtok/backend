package org.project.ttokttok.domain.applicant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
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
    private ApplicantPhase currentPhase;

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
        if (this.currentPhase != null && !isEvaluatingDocument()) {
            throw new InvalidPhaseTransitionException();
        }

        this.currentPhase = ApplicantPhase.DOCUMENT_EVALUATING;
        this.documentPhase = DocumentPhase.create(this, answers);
    }

    // 서류 상태 합격 설정
    public void passDocumentEvaluation() {
        //validateCurrentPhase(ApplicantPhase.DOCUMENT_EVALUATING);
        this.currentPhase = ApplicantPhase.DOCUMENT_PASS;
        this.documentPhase.updateStatus(PhaseStatus.PASS);
    }

    // 서류 상태 불합격 설정
    public void failDocumentEvaluation() {
        //validateCurrentPhase(ApplicantPhase.DOCUMENT_EVALUATING);
        this.currentPhase = ApplicantPhase.DOCUMENT_FAIL;
        this.documentPhase.updateStatus(PhaseStatus.FAIL);
    }

    // 서류 합격자 -> 면접 단계로 전환
    public void setInterviewPlan(LocalDate interviewDate) {
        validateCurrentPhase(ApplicantPhase.DOCUMENT_PASS);
        this.currentPhase = ApplicantPhase.INTERVIEW_EVALUATING;
        this.interviewPhase = InterviewPhase.create(this, interviewDate);
    }

    // 면접에 따라 합격 설정
    public void completeInterview(PhaseStatus result) {
        this.interviewPhase.updateStatus(result);

        // 면접 결과에 따라 최종 단계 결정
        if (result == PhaseStatus.PASS) {
            this.currentPhase = ApplicantPhase.INTERVIEW_PASS;
        } else if (result == PhaseStatus.FAIL) {
            this.currentPhase = ApplicantPhase.INTERVIEW_FAIL;
        }
    }

    // ----- 생성자 ----- //
    @Builder
    private Applicant(String userEmail,
                      String name,
                      Integer age,
                      String major,
                      String email,
                      String phone,
                      StudentStatus studentStatus,
                      Grade grade,
                      Gender gender,
                      ApplyForm applyForm) {
        this.userEmail = userEmail;
        this.name = name;
        this.age = age;
        this.major = major;
        this.email = email;
        this.phone = phone;
        this.studentStatus = studentStatus;
        this.grade = grade;
        this.gender = gender;
        this.currentPhase = ApplicantPhase.DOCUMENT_EVALUATING; // 기본 상태는 대기
        this.applyForm = applyForm;
    }

    public static Applicant createApplicant(String userEmail,
                                            String name,
                                            Integer age,
                                            String major,
                                            String email,
                                            String phone,
                                            StudentStatus studentStatus,
                                            Grade grade,
                                            Gender gender,
                                            ApplyForm applyForm) {
        return Applicant.builder()
                .userEmail(userEmail)
                .name(name)
                .age(age)
                .major(major)
                .email(email)
                .phone(phone)
                .studentStatus(studentStatus)
                .grade(grade)
                .gender(gender)
                .applyForm(applyForm)
                .build();
    }

    private void validateCurrentPhase(ApplicantPhase expectedPhase) {
        if (this.currentPhase != expectedPhase) {
            throw new InvalidPhaseTransitionException();
        }
    }

    public boolean canScheduleInterview() {
        return this.currentPhase == ApplicantPhase.DOCUMENT_PASS;
    }

    public boolean canEvaluateDocument() {
        return this.currentPhase == ApplicantPhase.DOCUMENT_EVALUATING;
    }

    public boolean canEvaluateInterview() {
        return this.currentPhase == ApplicantPhase.INTERVIEW_EVALUATING;
    }

    public boolean isEvaluatingDocument() {
        // 서류 평가 중이거나, 서류 불합격 상태에서 재지원 가능
        return this.currentPhase == ApplicantPhase.DOCUMENT_EVALUATING;
    }

    // 편의 메서드들
    public boolean isInDocumentPhase() {
        return currentPhase == ApplicantPhase.DOCUMENT_EVALUATING
                || currentPhase == ApplicantPhase.DOCUMENT_FAIL
                || currentPhase == ApplicantPhase.DOCUMENT_PASS;
    }

    public boolean isInInterviewPhase() {
        return currentPhase == ApplicantPhase.INTERVIEW_EVALUATING
                || currentPhase == ApplicantPhase.INTERVIEW_FAIL
                || currentPhase == ApplicantPhase.INTERVIEW_PASS;
    }

    public boolean hasInterviewPhase() {
        return interviewPhase != null;
    }
}
