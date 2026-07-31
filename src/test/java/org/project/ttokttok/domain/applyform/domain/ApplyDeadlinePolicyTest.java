package org.project.ttokttok.domain.applyform.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplyDeadlinePolicy - 마감 임박 판정")
class ApplyDeadlinePolicyTest {

    @Test
    @DisplayName("마감일이 없으면 마감 임박이 아니다")
    void isImminent_falseWhenNull() {
        assertThat(ApplyDeadlinePolicy.isImminent(null)).isFalse();
    }

    @ParameterizedTest(name = "마감까지 {0}일 남으면 마감 임박이다")
    @ValueSource(ints = {0, 1, 6, 7})
    @DisplayName("마감일이 오늘부터 7일 이내면 마감 임박이다 (경계 포함)")
    void isImminent_trueWithinSevenDays(int daysUntilDeadline) {
        LocalDate deadline = LocalDate.now().plusDays(daysUntilDeadline);

        assertThat(ApplyDeadlinePolicy.isImminent(deadline)).isTrue();
    }

    @Test
    @DisplayName("마감일이 8일 이후면 마감 임박이 아니다 (상한 경계)")
    void isImminent_falseWhenEightDaysAway() {
        assertThat(ApplyDeadlinePolicy.isImminent(LocalDate.now().plusDays(8))).isFalse();
    }

    @ParameterizedTest(name = "마감일이 {0}일 지났으면 마감 임박이 아니다")
    @ValueSource(ints = {1, 10})
    @DisplayName("마감일이 이미 지났으면 마감 임박이 아니다 (하한 경계)")
    void isImminent_falseWhenAlreadyPassed(int daysPassed) {
        LocalDate deadline = LocalDate.now().minusDays(daysPassed);

        assertThat(ApplyDeadlinePolicy.isImminent(deadline)).isFalse();
    }

    @Test
    @DisplayName("ApplyForm.isDeadlineImminent()은 동일한 규칙을 따른다")
    void applyFormDelegatesToPolicy() {
        ApplyForm imminentForm = ApplyForm.builder()
                .applyEndDate(LocalDate.now().plusDays(3))
                .build();
        ApplyForm farForm = ApplyForm.builder()
                .applyEndDate(LocalDate.now().plusDays(30))
                .build();

        assertThat(imminentForm.isDeadlineImminent()).isTrue();
        assertThat(farForm.isDeadlineImminent()).isFalse();
    }
}
