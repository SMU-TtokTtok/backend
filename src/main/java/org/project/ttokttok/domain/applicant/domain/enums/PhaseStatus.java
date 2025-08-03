package org.project.ttokttok.domain.applicant.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PhaseStatus {
    // 지원서 상태
    PASS("PASS"), // 합격
    FAIL("FAIL"), // 불합격
    EVALUATING("EVALUATING"); // 평가 중

    final String value;
}
