package org.project.ttokttok.domain.temp.applicant.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.project.ttokttok.domain.temp.applicant.domain.converter.TempAnswerListConverter;
import org.project.ttokttok.global.entity.BaseTimeEntity;

@Entity
@Table(name = "temp_applicants")
@Getter
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
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Convert(converter = TempAnswerListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> tempData;

    @Builder
    private TempApplicant(
            String formId,
            String userEmail,
            Map<String, Object> tempData
    ) {
        this.formId = formId;
        this.userEmail = userEmail;
        this.tempData = tempData;
    }

    public static TempApplicant create(
            String formId,
            String userEmail,
            Map<String, Object> tempData
    ) {
        return TempApplicant.builder()
                .formId(formId)
                .userEmail(userEmail)
                .tempData(tempData)
                .build();
    }

    /**
     * 임시 지원서 정보를 업데이트합니다.
     */
    public void update(Map<String, Object> tempData) {
        this.tempData = tempData;
    }
}
