package org.project.ttokttok.domain.applyform.domain;

import static org.project.ttokttok.global.exception.ErrorMessage.APPLY_FORM_GRADES_REQUIRED;
import static org.project.ttokttok.global.exception.ErrorMessage.APPLY_FORM_MAX_COUNT_INVALID;
import static org.project.ttokttok.global.exception.ErrorMessage.APPLY_FORM_QUESTIONS_REQUIRED;
import static org.project.ttokttok.global.exception.ErrorMessage.INTERVIEW_DATE_REQUIRED;
import static org.project.ttokttok.global.exception.ErrorMessage.INTERVIEW_INVALID_DATE_RANGE;
import static org.project.ttokttok.global.exception.ErrorMessage.RECRUIT_PERIOD_TOO_LONG;

import java.time.LocalDate;
import java.util.Collection;
import org.project.ttokttok.domain.applyform.exception.ApplyFormRuleException;
import org.project.ttokttok.domain.applyform.exception.InvalidDateRangeException;
import org.project.ttokttok.global.exception.ErrorMessage;

/**
 * 지원 폼 입력값 규칙. 폼 생성과 동아리 정보의 모집 기간 수정이 함께 쓴다.
 * 위반은 모두 400 이다.
 */
public final class ApplyFormInputPolicy {

    /** 모집 기간 최대 길이(개월). 마감일은 시작일의 이 개월 수 뒤 같은 날까지 허용한다. */
    public static final int MAX_RECRUIT_MONTHS = 3;
    private static final int MIN_APPLY_COUNT = 1;

    private ApplyFormInputPolicy() {
    }

    public static void validateRecruitPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException();
        }
        if (endDate.isAfter(startDate.plusMonths(MAX_RECRUIT_MONTHS))) {
            throw new ApplyFormRuleException(RECRUIT_PERIOD_TOO_LONG);
        }
    }

    public static void validateGrades(Collection<?> grades) {
        require(grades != null && !grades.isEmpty(), APPLY_FORM_GRADES_REQUIRED);
    }

    public static void validateQuestions(Collection<?> questions) {
        require(questions != null && !questions.isEmpty(), APPLY_FORM_QUESTIONS_REQUIRED);
    }

    public static void validateMaxApplyCount(int maxApplyCount) {
        require(maxApplyCount >= MIN_APPLY_COUNT, APPLY_FORM_MAX_COUNT_INVALID);
    }

    /** 면접이 있으면 일정이 모두 있어야 하고 시작일이 종료일보다 늦으면 안 된다. 모집 기간과는 겹쳐도 된다. */
    public static void validateInterview(boolean hasInterview, LocalDate startDate, LocalDate endDate) {
        if (!hasInterview) {
            return;
        }
        require(startDate != null && endDate != null, INTERVIEW_DATE_REQUIRED);
        require(!startDate.isAfter(endDate), INTERVIEW_INVALID_DATE_RANGE);
    }

    private static void require(boolean condition, ErrorMessage errorMessage) {
        if (!condition) {
            throw new ApplyFormRuleException(errorMessage);
        }
    }
}
