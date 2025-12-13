package org.project.ttokttok.domain.temp.applicant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.domain.json.AnswerListConverter;
import org.project.ttokttok.global.entity.BaseTimeEntity;

@Entity
@Table(name = "temp_applicants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TempApplicant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    // 사용자가 임시 저장한 지원 폼 ID
    @Column(name = "form_id", nullable = false, updatable = false)
    private String formId;

    // 학교 이메일
    private String userEmail;
    private String name;
    private Integer age;
    private String major;
    private String email; // 지원서에서 받은 이메일
    private String phone;

    // 작성되어 있지 않을 수도 있으니 null 가능하도록 설정
    @Enumerated(EnumType.STRING)
    private StudentStatus studentStatus;

    @Enumerated(EnumType.STRING)
    private Grade grade;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    // 임시 저장한 응답
    @Convert(converter = AnswerListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Answer> answers;
}
