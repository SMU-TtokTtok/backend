package org.project.ttokttok.domain.memo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.project.ttokttok.domain.applicant.domain.DocumentPhase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MemoTest {

    @Nested
    @DisplayName("create() 테스트")
    class CreateTest {

        @Test
        @DisplayName("서류 단계와 내용으로 메모를 생성할 수 있다")
        void create_Success() {
            // given
            DocumentPhase documentPhase = mock(DocumentPhase.class);
            String content = "면접 태도가 좋았음";

            // when
            Memo memo = Memo.create(documentPhase, content);

            // then
            assertThat(memo.getId()).isNotNull();
            assertThat(memo.getContent()).isEqualTo(content);
            assertThat(memo.getDocumentPhase()).isEqualTo(documentPhase);
        }

        @Test
        @DisplayName("생성되는 메모의 id는 매번 고유하다")
        void create_GeneratesUniqueId() {
            // given
            DocumentPhase documentPhase = mock(DocumentPhase.class);

            // when
            Memo memo1 = Memo.create(documentPhase, "내용1");
            Memo memo2 = Memo.create(documentPhase, "내용2");

            // then
            assertThat(memo1.getId()).isNotEqualTo(memo2.getId());
        }
    }

    @Nested
    @DisplayName("updateContent() 테스트")
    class UpdateContentTest {

        @Test
        @DisplayName("올바른 내용으로 메모 내용을 수정할 수 있다")
        void updateContent_Success() {
            // given
            Memo memo = Memo.create(mock(DocumentPhase.class), "원래 내용");
            String newContent = "수정된 내용";

            // when
            memo.updateContent(newContent);

            // then
            assertThat(memo.getContent()).isEqualTo(newContent);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("내용이 비어있으면 IllegalArgumentException이 발생한다")
        void updateContent_Fail_BlankContent(String blankContent) {
            // given
            Memo memo = Memo.create(mock(DocumentPhase.class), "원래 내용");

            // when & then
            assertThatThrownBy(() -> memo.updateContent(blankContent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메모 내용은 비워둘 수 없습니다.");
        }
    }
}
