package org.project.ttokttok.domain.applicant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.Status;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.memo.domain.Memo;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.util.ArrayList;
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

    // 지원한 사용자의 인증 정보 통해서 받은 이메일
    @Column(nullable = false)
    private String userEmail;

    // 고정 필드
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false)
    private String major;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    // 사용자 응답
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Answer> answers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applyform_id", nullable = false)
    private ApplyForm applyForm;

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Memo> memos = new ArrayList<>();

    // ----- 유틸 메서드 ----- //
    public void updateStatus(Status status) {
        this.status = status;
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
                      List<Answer> answers,
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
        this.status = Status.EVALUATING; // 기본 상태는 대기
        this.answers = answers != null ? answers : new ArrayList<>();
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
                                            List<Answer> answers,
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
                .answers(answers)
                .applyForm(applyForm)
                .build();
    }
}
