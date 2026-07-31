package org.project.ttokttok.domain.applicant.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApplicantPhaseQuery} 단위 테스트.
 *
 * <p>서류/면접 분기를 흩어진 삼항 연산자에서 이 전략으로 모았으므로,
 * 여기서 <b>어떤 kind 문자열이 어느 단계로 매핑되는지</b>와
 * <b>필터가 비어 있을 때 조건을 만들지 않는지</b>를 고정한다.
 * 실제 쿼리 결과는 {@code ApplicantCustomRepositoryImplTest} 가 검증한다.
 */
@DisplayName("ApplicantPhaseQuery - 전형 단계 조회 전략")
class ApplicantPhaseQueryTest {

    @Nested
    @DisplayName("from(): kind 문자열 매핑")
    class From {

        @ParameterizedTest(name = "\"{0}\" -> INTERVIEW")
        @ValueSource(strings = {"INTERVIEW", "interview", "Interview"})
        @DisplayName("INTERVIEW는 대소문자 무관하게 면접 전형으로 매핑된다")
        void mapsInterviewIgnoringCase(String kind) {
            assertThat(ApplicantPhaseQuery.from(kind)).isEqualTo(ApplicantPhaseQuery.INTERVIEW);
        }

        @ParameterizedTest(name = "\"{0}\" -> DOCUMENT")
        @ValueSource(strings = {"DOCUMENT", "document", "", "그 외 아무 값"})
        @DisplayName("INTERVIEW가 아닌 값은 모두 서류 전형으로 매핑된다")
        void mapsEverythingElseToDocument(String kind) {
            assertThat(ApplicantPhaseQuery.from(kind)).isEqualTo(ApplicantPhaseQuery.DOCUMENT);
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("kind가 null이어도 예외 없이 서류 전형으로 매핑된다")
        void mapsNullToDocumentWithoutThrowing(String kind) {
            assertThat(ApplicantPhaseQuery.from(kind)).isEqualTo(ApplicantPhaseQuery.DOCUMENT);
        }
    }

    @Nested
    @DisplayName("statusEq(): 상태 필터")
    class StatusEq {

        @Test
        @DisplayName("status가 null이면 조건을 만들지 않는다")
        void nullStatusProducesNoCondition() {
            assertThat(ApplicantPhaseQuery.DOCUMENT.statusEq(null)).isNull();
            assertThat(ApplicantPhaseQuery.INTERVIEW.statusEq(null)).isNull();
        }

        @Test
        @DisplayName("모든 PhaseStatus 값에 대해 조건을 만든다")
        void everyStatusProducesCondition() {
            for (PhaseStatus status : PhaseStatus.values()) {
                assertThat(ApplicantPhaseQuery.DOCUMENT.statusEq(status))
                        .as("DOCUMENT/%s", status).isNotNull();
                assertThat(ApplicantPhaseQuery.INTERVIEW.statusEq(status))
                        .as("INTERVIEW/%s", status).isNotNull();
            }
        }

        @Test
        @DisplayName("서류와 면접은 서로 다른 필드를 참조한다")
        void documentAndInterviewReferenceDifferentFields() {
            String documentCondition = ApplicantPhaseQuery.DOCUMENT.statusEq(PhaseStatus.PASS).toString();
            String interviewCondition = ApplicantPhaseQuery.INTERVIEW.statusEq(PhaseStatus.PASS).toString();

            assertThat(documentCondition).contains("documentPhase");
            assertThat(interviewCondition).contains("interviewPhase");
            assertThat(documentCondition).isNotEqualTo(interviewCondition);
        }
    }

    @Nested
    @DisplayName("evaluatingOnly(): 평가중 필터")
    class EvaluatingOnly {

        @Test
        @DisplayName("false면 조건을 만들지 않는다")
        void falseProducesNoCondition() {
            assertThat(ApplicantPhaseQuery.DOCUMENT.evaluatingOnly(false)).isNull();
            assertThat(ApplicantPhaseQuery.INTERVIEW.evaluatingOnly(false)).isNull();
        }

        @Test
        @DisplayName("true면 EVALUATING 상태 조건과 같다")
        void trueEqualsEvaluatingStatusCondition() {
            for (ApplicantPhaseQuery phaseQuery : ApplicantPhaseQuery.values()) {
                assertThat(phaseQuery.evaluatingOnly(true))
                        .as("%s", phaseQuery)
                        .isEqualTo(phaseQuery.statusEq(PhaseStatus.EVALUATING));
            }
        }
    }

    @Nested
    @DisplayName("phaseLink(): 지원자-전형 연결 조건")
    class PhaseLink {

        @Test
        @DisplayName("단계마다 서로 다른 전형 테이블을 연결한다")
        void linksDifferentPhaseTables() {
            assertThat(ApplicantPhaseQuery.DOCUMENT.phaseLink().toString()).contains("documentPhase");
            assertThat(ApplicantPhaseQuery.INTERVIEW.phaseLink().toString()).contains("interviewPhase");
        }
    }
}
