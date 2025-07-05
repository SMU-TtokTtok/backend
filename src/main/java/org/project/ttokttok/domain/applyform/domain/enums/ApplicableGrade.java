package org.project.ttokttok.domain.applyform.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplicableGrade {
    FIRST_GRADE("1학년"),   // 1학년
    SECOND_GRADE("2학년"),  // 2학년
    THIRD_GRADE("3학년"),   // 3학년
    FOURTH_GRADE("4학년");  // 4학년

    // 숫자로 바꾸기
    final String grade;
}
