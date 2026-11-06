package org.project.ttokttok.domain.applicant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicationHistorySort;

class ApplicationHistorySortTest {
    @Test
    void parsesSupportedValuesAndKeepsDefaultForUnknownValues() {
        assertThat(ApplicationHistorySort.from("latest")).isEqualTo(ApplicationHistorySort.LATEST);
        assertThat(ApplicationHistorySort.from("popular")).isEqualTo(ApplicationHistorySort.POPULAR);
        assertThat(ApplicationHistorySort.from("member_count")).isEqualTo(ApplicationHistorySort.MEMBER_COUNT);
        assertThat(ApplicationHistorySort.from("unknown")).isEqualTo(ApplicationHistorySort.LATEST);
        assertThat(ApplicationHistorySort.from("")).isEqualTo(ApplicationHistorySort.LATEST);
        assertThat(ApplicationHistorySort.from(null)).isEqualTo(ApplicationHistorySort.LATEST);
    }
}
