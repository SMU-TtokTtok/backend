package org.project.ttokttok.domain.applyform.repository;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

import com.querydsl.core.types.dsl.BooleanExpression;
import java.time.LocalDate;
import org.project.ttokttok.domain.applyform.domain.QApplyForm;

/**
 * 쿼리에서 쓰는 모집 상태 조건. {@code RecruitStatePolicy} 의 OPEN 판정과 같은 규칙이다.
 */
public final class ApplyFormQueryConditions {

    private ApplyFormQueryConditions() {
    }

    /** 활성, 모집 스위치 켜짐, 시작일 <= today <= 마감일. */
    public static BooleanExpression openOn(QApplyForm form, LocalDate today) {
        return form.status.eq(ACTIVE)
                .and(form.isRecruiting.isTrue())
                .and(form.applyStartDate.loe(today))
                .and(form.applyEndDate.goe(today));
    }
}
