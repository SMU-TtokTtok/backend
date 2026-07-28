package org.project.ttokttok.domain.applyform.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 지원 마감 임박 판정 규칙
 *
 * <p>"마감 임박"의 정의를 한 곳에 모아둔 정책 클래스입니다.
 * 엔티티를 손에 쥔 호출부는 {@link ApplyForm#isDeadlineImminent()} 를 사용하고,
 * QueryDSL projection DTO 처럼 마감일만 가진 호출부는 이 클래스를 직접 사용합니다.
 * 어느 경로로 들어오든 판정 규칙은 하나입니다.
 */
public final class ApplyDeadlinePolicy {

    /** 마감일까지 남은 일수가 이 값 이하이면 마감 임박으로 본다. */
    private static final int IMMINENT_DAYS = 7;

    private ApplyDeadlinePolicy() {
    }

    /**
     * 마감 임박 여부를 판정한다.
     *
     * @param applyEndDate 지원 마감일 (null 이면 마감 임박 아님)
     * @return 오늘(당일 포함)부터 마감일까지 남은 일수가 0 이상 {@value #IMMINENT_DAYS} 이하이면 true
     */
    public static boolean isImminent(LocalDate applyEndDate) {
        if (applyEndDate == null) {
            return false;
        }

        long daysUntilDeadline = LocalDate.now().until(applyEndDate, ChronoUnit.DAYS);

        return daysUntilDeadline >= 0 && daysUntilDeadline <= IMMINENT_DAYS;
    }
}
