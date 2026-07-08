package org.project.ttokttok.domain.applicant.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.memo.domain.Memo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DocumentPhaseTest {

    @Nested
    @DisplayName("create() 테스트")
    class CreateTest {

        @Test
        @DisplayName("지원자와 답변 목록으로 서류 단계를 생성하면 상태는 EVALUATING이다")
        void create_Success() {
            // given
            Applicant applicant = mock(Applicant.class);
            List<Answer> answers = List.of();

            // when
            DocumentPhase documentPhase = DocumentPhase.create(applicant, answers);

            // then
            assertThat(documentPhase.getApplicant()).isEqualTo(applicant);
            assertThat(documentPhase.getAnswers()).isEqualTo(answers);
            assertThat(documentPhase.getStatus()).isEqualTo(PhaseStatus.EVALUATING);
            assertThat(documentPhase.getMemos()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateStatus() 테스트")
    class UpdateStatusTest {

        @Test
        @DisplayName("상태를 변경할 수 있다")
        void updateStatus_Success() {
            // given
            DocumentPhase documentPhase = DocumentPhase.create(mock(Applicant.class), List.of());

            // when
            documentPhase.updateStatus(PhaseStatus.PASS);

            // then
            assertThat(documentPhase.getStatus()).isEqualTo(PhaseStatus.PASS);
        }
    }

    @Nested
    @DisplayName("메모 관련 비즈니스 메서드 테스트")
    class MemoTest {

        @Test
        @DisplayName("addMemo() 호출 시 메모가 추가되고 생성된 memoId가 반환된다")
        void addMemo_Success() {
            // given
            DocumentPhase documentPhase = DocumentPhase.create(mock(Applicant.class), List.of());

            // when
            String memoId = documentPhase.addMemo("면접 태도가 좋았음");

            // then
            assertThat(memoId).isNotNull();
            assertThat(documentPhase.getMemos()).hasSize(1);
            assertThat(documentPhase.getMemos().get(0).getId()).isEqualTo(memoId);
            assertThat(documentPhase.getMemos().get(0).getContent()).isEqualTo("면접 태도가 좋았음");
        }

        @Test
        @DisplayName("updateMemo() 호출 시 해당 memoId의 메모 내용이 수정된다")
        void updateMemo_Success() {
            // given
            DocumentPhase documentPhase = DocumentPhase.create(mock(Applicant.class), List.of());
            String memoId = documentPhase.addMemo("원래 내용");

            // when
            documentPhase.updateMemo(memoId, "수정된 내용");

            // then
            Memo updated = documentPhase.getMemos().get(0);
            assertThat(updated.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("존재하지 않는 memoId로 updateMemo()를 호출해도 예외 없이 아무 일도 일어나지 않는다")
        void updateMemo_NoOp_WhenMemoNotFound() {
            // given
            DocumentPhase documentPhase = DocumentPhase.create(mock(Applicant.class), List.of());
            documentPhase.addMemo("원래 내용");

            // when
            documentPhase.updateMemo("존재하지-않는-id", "수정된 내용");

            // then
            assertThat(documentPhase.getMemos()).hasSize(1);
            assertThat(documentPhase.getMemos().get(0).getContent()).isEqualTo("원래 내용");
        }

        @Test
        @DisplayName("deleteMemo() 호출 시 해당 memoId의 메모가 삭제된다")
        void deleteMemo_Success() {
            // given
            DocumentPhase documentPhase = DocumentPhase.create(mock(Applicant.class), List.of());
            String memoId = documentPhase.addMemo("삭제될 메모");

            // when
            documentPhase.deleteMemo(memoId);

            // then
            assertThat(documentPhase.getMemos()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 memoId로 deleteMemo()를 호출해도 예외 없이 기존 메모는 유지된다")
        void deleteMemo_NoOp_WhenMemoNotFound() {
            // given
            DocumentPhase documentPhase = DocumentPhase.create(mock(Applicant.class), List.of());
            documentPhase.addMemo("유지될 메모");

            // when
            documentPhase.deleteMemo("존재하지-않는-id");

            // then
            assertThat(documentPhase.getMemos()).hasSize(1);
        }

        @Test
        @DisplayName("getMemos()는 방어적 복사본을 반환하여 외부에서 수정해도 내부 상태에 영향을 주지 않는다")
        void getMemos_ReturnsDefensiveCopy() {
            // given
            DocumentPhase documentPhase = DocumentPhase.create(mock(Applicant.class), List.of());
            documentPhase.addMemo("메모");

            // when
            documentPhase.getMemos().clear();

            // then
            assertThat(documentPhase.getMemos()).hasSize(1);
        }
    }
}
