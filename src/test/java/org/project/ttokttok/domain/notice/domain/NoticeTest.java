package org.project.ttokttok.domain.notice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoticeTest {

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("제목과 내용으로 공지를 생성하면 조회수는 0으로 초기화된다.")
        void createSuccess() {
            // when
            Notice notice = Notice.create("제목", "내용");

            // then
            assertThat(notice.getTitle()).isEqualTo("제목");
            assertThat(notice.getContent()).isEqualTo("내용");
            assertThat(notice.getViewCount()).isZero();
        }

        @Test
        @DisplayName("제목이 공백이면 예외가 발생한다.")
        void createBlankTitle() {
            assertThatThrownBy(() -> Notice.create("   ", "내용"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("내용이 null이면 예외가 발생한다.")
        void createNullContent() {
            assertThatThrownBy(() -> Notice.create("제목", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("increaseViewCount()")
    class IncreaseViewCount {

        @Test
        @DisplayName("호출할 때마다 조회수가 1씩 증가한다.")
        void increase() {
            // given
            Notice notice = Notice.create("제목", "내용");

            // when
            notice.increaseViewCount();
            notice.increaseViewCount();

            // then
            assertThat(notice.getViewCount()).isEqualTo(2);
        }
    }
}
