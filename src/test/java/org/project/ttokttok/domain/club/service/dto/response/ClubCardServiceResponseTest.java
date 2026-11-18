package org.project.ttokttok.domain.club.service.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;

@DisplayName("ClubCardServiceResponse - 마감 임박")
class ClubCardServiceResponseTest {

    private ClubCardQueryResponse card(boolean recruiting, LocalDate deadline) {
        return new ClubCardQueryResponse(
                "1", "동아리", ClubType.CENTRAL, ClubCategory.ACADEMIC,
                null, "요약", "http://img", 10, recruiting, false, false, deadline);
    }

    @Test
    @DisplayName("모집 중이고 마감이 7일 이내면 마감 임박이다")
    void recruitingAndDeadlineSoon_isImminent() {
        ClubCardServiceResponse response = ClubCardServiceResponse.from(card(true, LocalDate.now().plusDays(3)));

        assertThat(response.isDeadlineImminent()).isTrue();
    }

    @Test
    @DisplayName("모집 중이어도 마감이 7일보다 멀면 마감 임박이 아니다")
    void recruitingButDeadlineFar_isNotImminent() {
        ClubCardServiceResponse response = ClubCardServiceResponse.from(card(true, LocalDate.now().plusDays(30)));

        assertThat(response.isDeadlineImminent()).isFalse();
    }

    @Test
    @DisplayName("모집 중이 아니면 마감일이 가까워도 마감 임박이 아니다 - 수동으로 마감한 동아리")
    void notRecruiting_isNotImminentEvenIfDeadlineSoon() {
        ClubCardServiceResponse response = ClubCardServiceResponse.from(card(false, LocalDate.now().plusDays(3)));

        assertThat(response.isDeadlineImminent()).isFalse();
    }
}
