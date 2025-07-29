package org.project.ttokttok.domain.applicant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.project.ttokttok.domain.applicant.controller.enums.Kind;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.Status;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.memo.domain.Memo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("DOCUMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentApplicant extends BaseApplicant {

    // 서류 지원자만 answers 필드 가짐
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Answer> answers;

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Memo> memos = new ArrayList<>();

    @Builder
    private DocumentApplicant(String userEmail, String name, Integer age, String major,
                              String email, String phone, StudentStatus studentStatus,
                              Grade grade, Gender gender, List<Answer> answers, ApplyForm applyForm) {

        super(userEmail, name, age, major, email, phone, studentStatus, grade, gender, applyForm);
        this.answers = answers != null ? answers : new ArrayList<>();
    }

    @Override
    public Kind getKind() {
        return Kind.DOCUMENT;
    }

    // ----- 연관관계 편의 메서드 ----- //
    public String addMemo(String content) {
        Memo memo = Memo.create(this, content);
        this.memos.add(memo);

        return memo.getId();
    }

    public void updateMemo(String memoId, String content) {
        this.memos.stream()
                .filter(memo -> memo.getId().equals(memoId))
                .findFirst()
                .ifPresent(memo -> memo.updateContent(content));
    }

    public void deleteMemo(String memoId) {
        this.memos.removeIf(memo -> memo.getId().equals(memoId));
    }

    public InterviewApplicant createInterviewApplicant(ApplyForm interviewForm, LocalDate interviewDate) {
        return InterviewApplicant.builder()
                .userEmail(this.getUserEmail())
                .name(this.getName())
                .age(this.getAge())
                .major(this.getMajor())
                .email(this.getEmail())
                .phone(this.getPhone())
                .studentStatus(this.getStudentStatus())
                .grade(this.getGrade())
                .gender(this.getGender())
                .applyForm(interviewForm)
                .documentApplicantId(this.getId())
                .interviewDate(interviewDate)
                .build();
    }
}
