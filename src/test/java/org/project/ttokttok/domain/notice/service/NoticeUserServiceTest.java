package org.project.ttokttok.domain.notice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeDetailResponse;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeListResponse;
import org.project.ttokttok.domain.notice.domain.Notice;
import org.project.ttokttok.domain.notice.exception.NoticeNotFoundException;
import org.project.ttokttok.domain.notice.repository.NoticeRepository;
import org.project.ttokttok.domain.notice.repository.dto.NoticePageQueryResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeUserServiceTest {

    private final NoticeRepository noticeRepository = mock(NoticeRepository.class);
    private final NoticeUserService noticeUserService = new NoticeUserService(noticeRepository);

    @Nested
    @DisplayName("getNotices()")
    class GetNotices {

        @Test
        @DisplayName("목록 조회 시 페이지 정보와 공지 요약 목록을 반환한다.")
        void getNoticesSuccess() {
            // given
            Notice notice1 = Notice.create("제목1", "내용1");
            Notice notice2 = Notice.create("제목2", "내용2");
            NoticePageQueryResponse queryResponse = NoticePageQueryResponse.builder()
                    .content(List.of(notice1, notice2))
                    .totalCount(2L)
                    .build();
            when(noticeRepository.searchNotices(1, 10, null)).thenReturn(queryResponse);

            // when
            NoticeListResponse response = noticeUserService.getNotices(1, 10, null);

            // then
            assertThat(response.currentPage()).isEqualTo(1);
            assertThat(response.totalPage()).isEqualTo(1);
            assertThat(response.totalCount()).isEqualTo(2);
            assertThat(response.notices()).hasSize(2);
            assertThat(response.notices().get(0).title()).isEqualTo("제목1");
        }

        @Test
        @DisplayName("총 건수가 페이지 크기보다 많으면 전체 페이지 수가 올림 계산된다.")
        void getNoticesTotalPage() {
            // given
            NoticePageQueryResponse queryResponse = NoticePageQueryResponse.builder()
                    .content(List.of())
                    .totalCount(21L)
                    .build();
            when(noticeRepository.searchNotices(1, 10, "검색어")).thenReturn(queryResponse);

            // when
            NoticeListResponse response = noticeUserService.getNotices(1, 10, "검색어");

            // then
            assertThat(response.totalPage()).isEqualTo(3); // ceil(21 / 10)
            assertThat(response.totalCount()).isEqualTo(21);
        }
    }

    @Nested
    @DisplayName("getNoticeDetail()")
    class GetNoticeDetail {

        @Test
        @DisplayName("상세 조회 시 조회수를 증가시키고 상세 응답을 반환한다.")
        void getNoticeDetailSuccess() {
            // given
            Notice notice = mock(Notice.class);
            when(notice.getId()).thenReturn("notice-1");
            when(notice.getTitle()).thenReturn("제목");
            when(notice.getContent()).thenReturn("내용");
            when(notice.getViewCount()).thenReturn(1);
            when(noticeRepository.findById("notice-1")).thenReturn(Optional.of(notice));

            // when
            NoticeDetailResponse response = noticeUserService.getNoticeDetail("notice-1");

            // then
            verify(notice).increaseViewCount();
            assertThat(response.noticeId()).isEqualTo("notice-1");
            assertThat(response.content()).isEqualTo("내용");
            assertThat(response.viewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("존재하지 않는 공지를 조회하면 예외가 발생한다.")
        void getNoticeDetailNotFound() {
            // given
            when(noticeRepository.findById("missing")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> noticeUserService.getNoticeDetail("missing"))
                    .isInstanceOf(NoticeNotFoundException.class);
        }
    }
}
