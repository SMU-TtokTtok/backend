package org.project.ttokttok.domain.applicant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
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
import org.project.ttokttok.global.entity.BaseTimeEntity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applyform_id", nullable = false)
    private ApplyForm applyForm;

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
}
