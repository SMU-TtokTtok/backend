package org.project.ttokttok.domain.notice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.notice.controller.dto.request.CreateNoticeRequest;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeCreateResponse;
import org.project.ttokttok.domain.notice.service.NoticeAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeAdminControllerTest {

    private final NoticeAdminService noticeAdminService = mock(NoticeAdminService.class);
    private final NoticeAdminController noticeAdminController = new NoticeAdminController(noticeAdminService);

    @Test
    @DisplayName("공지 저장 요청 시 201과 생성된 공지 ID를 반환한다.")
    void createNotice() {
        // given
        when(noticeAdminService.createNotice(any())).thenReturn("notice-1");
        CreateNoticeRequest request = new CreateNoticeRequest("제목", "내용");

        // when
        ResponseEntity<NoticeCreateResponse> response =
                noticeAdminController.createNotice("ttok_operator", request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().noticeId()).isEqualTo("notice-1");
    }
}
