package org.project.ttokttok.domain.applyform.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.INACTIVE;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;

class RecruitStatePolicyTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final LocalDate END = LocalDate.of(2026, 11, 30);

    private RecruitState stateOn(LocalDate today) {
        return RecruitStatePolicy.of(ACTIVE, true, START, END, today);
    }

    @Test
    void 시작일_전이면_모집_전() {
        assertThat(stateOn(START.minusDays(1))).isEqualTo(RecruitState.UPCOMING);
    }

    @Test
    void 시작일과_마감일_당일은_모집_중() {
        assertThat(stateOn(START)).isEqualTo(RecruitState.OPEN);
        assertThat(stateOn(END)).isEqualTo(RecruitState.OPEN);
    }

    @Test
    void 마감일_다음날은_마감() {
        assertThat(stateOn(END.plusDays(1))).isEqualTo(RecruitState.CLOSED);
    }

    @Test
    void 모집_스위치가_꺼지면_기간_안이어도_마감() {
        assertThat(RecruitStatePolicy.of(ACTIVE, false, START, END, START)).isEqualTo(RecruitState.CLOSED);
    }

    @Test
    void 비활성_폼은_종료() {
        assertThat(RecruitStatePolicy.of(INACTIVE, true, START, END, START)).isEqualTo(RecruitState.FINISHED);
    }
}
