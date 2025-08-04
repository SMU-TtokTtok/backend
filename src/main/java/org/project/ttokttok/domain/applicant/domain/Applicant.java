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
        this.currentPhase = ApplicantPhase.DOCUMENT; // 기본 상태는 서류 단계
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

    // 비즈니스 메서드
    // 초기 상태이거나, 재지원 가능한 상태인지 확인
    public void submitDocument(List<Answer> answers) {

        if (this.currentPhase != null && isInInterviewPhase()) {
            throw new InvalidPhaseTransitionException();
        }

        this.currentPhase = ApplicantPhase.DOCUMENT;
        this.documentPhase = DocumentPhase.create(this, answers);
    }

    // 서류 상태 합격 설정
    public void passDocumentEvaluation() {
        this.documentPhase.updateStatus(PhaseStatus.PASS);
    }

    // 최종 합격 처리 -> 면접 단계 생성
    public void updateToInterviewPhase(LocalDate interviewDate) {
        this.interviewPhase = InterviewPhase.create(this, interviewDate);
        this.currentPhase = ApplicantPhase.INTERVIEW;
    }

    // 서류 상태 불합격 설정
    public void failDocumentEvaluation() {
        if (this.applyForm.isHasInterview() && this.interviewPhase != null) {
            throw new IllegalArgumentException("이미 면접 단계에 존재하는 지원자입니다.");
        }

        this.documentPhase.updateStatus(PhaseStatus.FAIL);
    }

    // 면접에 따라 합격 설정
    public void passInterview() {
        this.interviewPhase.updateStatus(PhaseStatus.PASS);
    }

    public void failInterview() {
        this.interviewPhase.updateStatus(PhaseStatus.FAIL);
    }

    // 편의 메서드들
    public boolean isInDocumentPhase() {
        return currentPhase == ApplicantPhase.DOCUMENT;
    }

    public boolean isInInterviewPhase() {
        return currentPhase == ApplicantPhase.INTERVIEW;
    }

    public boolean hasInterviewPhase() {
        return this.applyForm.isHasInterview();
    }

    public void setDocumentEvaluating() {
        if (this.applyForm.isHasInterview() && this.interviewPhase != null) {
            throw new IllegalArgumentException("이미 면접 단계에 존재하는 지원자입니다.");
        }

        this.documentPhase.updateStatus(PhaseStatus.EVALUATING);
    }

    public void setInterviewEvaluating() {
        this.interviewPhase.updateStatus(PhaseStatus.EVALUATING);
    }
}
