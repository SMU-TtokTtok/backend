package org.project.ttokttok.domain.applyform.domain;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.INACTIVE;

import java.time.LocalDate;
import java.time.ZoneId;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;

/**
 * 모집 상태 판정 규칙
 *
 * <p>제출, 임시저장, 목록 표시, 지원자 초기화가 모두 이 판정 하나를 쓴다.
 * 엔티티를 가진 호출부는 {@link ApplyForm#recruitState()} 를, QueryDSL projection 처럼
 * 필드만 가진 호출부는 {@link #of} 를 직접 쓴다.
 *
 * <p>모집 기간은 한국 시간 기준으로 시작일 0시부터 마감일 23:59 까지다.
 */
public final class RecruitStatePolicy {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private RecruitStatePolicy() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static RecruitState of(ApplyFormStatus status, boolean isRecruiting,
                                  LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (status == INACTIVE) {
            return RecruitState.FINISHED;
        }
        if (!isRecruiting || today.isAfter(endDate)) {
            return RecruitState.CLOSED;
        }
        return today.isBefore(startDate) ? RecruitState.UPCOMING : RecruitState.OPEN;
    }
}
