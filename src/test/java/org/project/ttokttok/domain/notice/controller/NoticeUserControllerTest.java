package org.project.ttokttok.domain.notice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeDetailResponse;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeListResponse;
import org.project.ttokttok.domain.notice.service.NoticeUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeUserControllerTest {

    private final NoticeUserService noticeUserService = mock(NoticeUserService.class);
    private final NoticeUserController noticeUserController = new NoticeUserController(noticeUserService);

    @Test
    @DisplayName("목록 조회 요청 시 200과 목록 응답을 반환한다.")
    void getNotices() {
        // given
        NoticeListResponse listResponse = NoticeListResponse.of(1, 1, 0, List.of());
        when(noticeUserService.getNotices(1, 10, null)).thenReturn(listResponse);

        // when
        ResponseEntity<NoticeListResponse> response = noticeUserController.getNotices(1, 10, null);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(listResponse);
    }

    @Test
    @DisplayName("상세 조회 요청 시 200과 상세 응답을 반환한다.")
    void getNoticeDetail() {
        // given
        NoticeDetailResponse detailResponse =
                new NoticeDetailResponse("notice-1", "제목", "내용", "ttok_operator", LocalDateTime.now(), 1);
        when(noticeUserService.getNoticeDetail("notice-1")).thenReturn(detailResponse);

        // when
        ResponseEntity<NoticeDetailResponse> response = noticeUserController.getNoticeDetail("notice-1");

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detailResponse);
    }
}
