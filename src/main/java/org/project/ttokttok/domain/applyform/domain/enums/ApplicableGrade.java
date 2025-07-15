package org.project.ttokttok.domain.applyform.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplicableGrade {
    FIRST_GRADE(1),   // 1학년
    SECOND_GRADE(2),  // 2학년
    THIRD_GRADE(3),   // 3학년
    FOURTH_GRADE(4);  // 4학년

    final int grade;

    public static ApplicableGrade from(int grade) {
        for (ApplicableGrade applicableGrade : values()) {
            if (applicableGrade.grade == grade) {
                return applicableGrade;
            }
        }
        throw new IllegalArgumentException("잘못된 학년입니다." + grade);
    }
}
