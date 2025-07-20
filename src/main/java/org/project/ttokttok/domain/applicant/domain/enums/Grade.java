package org.project.ttokttok.domain.applicant.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Grade {
    // 지원자 학년 표현용 열거형
    FIRST_GRADE(1),
    SECOND_GRADE(2),
    THIRD_GRADE(3),
    FOURTH_GRADE(4);

    final int grade;
}
