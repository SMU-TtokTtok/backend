package org.project.ttokttok.domain.club.service.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ClubDetailServiceResponse#from(ClubDetailQueryResponse)} 변환 검증.
 *
 * <p>{@code ClubUserServiceTest} 는 이 정적 메서드를 목으로 대체하므로 실제 변환 로직이
 * 검증되지 않았다. 특히 마감 임박 계산이 {@code ApplyDeadlinePolicy} 로 옮겨진 뒤
 * 동일한 결과를 내는지 확인할 곳이 없어 여기서 직접 검증한다.
 */
@DisplayName("ClubDetailServiceResponse - 조회 결과 변환")
class ClubDetailServiceResponseTest {

    private ClubDetailQueryResponse givenQueryResponse(LocalDate applyDeadLine) {
        return new ClubDetailQueryResponse(
                "테스트동아리",
                ClubType.CENTRAL,
                ClubCategory.ACADEMIC,
                null,
                true,
                true,
                "한줄 소개",
                "https://image/profile.png",
                12,
                LocalDate.now().minusDays(3),
                applyDeadLine,
                Set.of(ApplicableGrade.FIRST_GRADE),
                50,
                "동아리 소개 내용"
        );
    }

    @Nested
    @DisplayName("마감 임박 판정")
    class DeadlineImminent {

        @ParameterizedTest(name = "마감까지 {0}일 남으면 마감 임박이다")
        @ValueSource(ints = {0, 1, 7})
        @DisplayName("마감일이 오늘부터 7일 이내면 마감 임박이다 (경계 포함)")
        void trueWithinSevenDays(int daysUntilDeadline) {
            ClubDetailServiceResponse response =
                    ClubDetailServiceResponse.from(givenQueryResponse(LocalDate.now().plusDays(daysUntilDeadline)));

            assertThat(response.isDeadlineImminent()).isTrue();
        }

        @Test
        @DisplayName("마감일이 8일 이후면 마감 임박이 아니다")
        void falseWhenEightDaysAway() {
            ClubDetailServiceResponse response =
                    ClubDetailServiceResponse.from(givenQueryResponse(LocalDate.now().plusDays(8)));

            assertThat(response.isDeadlineImminent()).isFalse();
        }

        @Test
        @DisplayName("마감일이 이미 지났으면 마감 임박이 아니다")
        void falseWhenAlreadyPassed() {
            ClubDetailServiceResponse response =
                    ClubDetailServiceResponse.from(givenQueryResponse(LocalDate.now().minusDays(1)));

            assertThat(response.isDeadlineImminent()).isFalse();
        }

        @Test
        @DisplayName("마감일 정보가 없으면 마감 임박이 아니다")
        void falseWhenNoDeadline() {
            ClubDetailServiceResponse response = ClubDetailServiceResponse.from(givenQueryResponse(null));

            assertThat(response.isDeadlineImminent()).isFalse();
        }
    }

    @Nested
    @DisplayName("나머지 필드 매핑")
    class FieldMapping {

        @Test
        @DisplayName("조회 결과의 값을 그대로 옮긴다")
        void copiesQueryValues() {
            LocalDate deadline = LocalDate.now().plusDays(3);
            ClubDetailQueryResponse queryResponse = givenQueryResponse(deadline);

            ClubDetailServiceResponse response = ClubDetailServiceResponse.from(queryResponse);

            assertThat(response.name()).isEqualTo("테스트동아리");
            assertThat(response.clubType()).isEqualTo(ClubType.CENTRAL);
            assertThat(response.clubCategory()).isEqualTo(ClubCategory.ACADEMIC);
            assertThat(response.bookmarked()).isTrue();
            assertThat(response.recruiting()).isTrue();
            assertThat(response.summary()).isEqualTo("한줄 소개");
            assertThat(response.profileImageUrl()).isEqualTo("https://image/profile.png");
            assertThat(response.clubMemberCount()).isEqualTo(12);
            assertThat(response.grades()).containsExactly(ApplicableGrade.FIRST_GRADE);
            assertThat(response.maxApplyCount()).isEqualTo(50);
            assertThat(response.content()).isEqualTo("동아리 소개 내용");
        }

        @Test
        @DisplayName("LocalDate 날짜는 그날의 자정 LocalDateTime으로 변환된다")
        void convertsDatesToStartOfDay() {
            LocalDate deadline = LocalDate.now().plusDays(3);

            ClubDetailServiceResponse response = ClubDetailServiceResponse.from(givenQueryResponse(deadline));

            assertThat(response.applyDeadLine()).isEqualTo(deadline.atStartOfDay());
            assertThat(response.applyStartDate()).isEqualTo(LocalDate.now().minusDays(3).atStartOfDay());
        }

        @Test
        @DisplayName("마감일이 없으면 applyDeadLine도 null이다")
        void nullDeadlineStaysNull() {
            ClubDetailServiceResponse response = ClubDetailServiceResponse.from(givenQueryResponse(null));

            assertThat(response.applyDeadLine()).isNull();
        }
    }
}
