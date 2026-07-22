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
        @DisplayName("제목과 내용으로 공지를 생성하면 조회수는 0으로 초기화되고 작성자가 설정된다.")
        void createSuccess() {
            // when
            Notice notice = Notice.create("제목", "내용", "ttok_operator");

            // then
            assertThat(notice.getTitle()).isEqualTo("제목");
            assertThat(notice.getContent()).isEqualTo("내용");
            assertThat(notice.getCreatedBy()).isEqualTo("ttok_operator");
            assertThat(notice.getViewCount()).isZero();
        }

        @Test
        @DisplayName("제목이 공백이면 예외가 발생한다.")
        void createBlankTitle() {
            assertThatThrownBy(() -> Notice.create("   ", "내용", "ttok_operator"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("내용이 null이면 예외가 발생한다.")
        void createNullContent() {
            assertThatThrownBy(() -> Notice.create("제목", null, "ttok_operator"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
