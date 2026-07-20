package org.project.ttokttok.domain.notice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.notice.domain.Notice;
import org.project.ttokttok.domain.notice.repository.NoticeRepository;
import org.project.ttokttok.domain.notice.service.dto.request.CreateNoticeServiceRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeAdminServiceTest {

    private final NoticeRepository noticeRepository = mock(NoticeRepository.class);
    private final NoticeAdminService noticeAdminService = new NoticeAdminService(noticeRepository);

    @Test
    @DisplayName("공지사항 저장에 성공하면 저장된 공지의 ID를 반환한다.")
    void createNoticeSuccess() {
        // given
        CreateNoticeServiceRequest request =
                new CreateNoticeServiceRequest("ttok_operator", "공지 제목", "공지 내용");

        Notice saved = mock(Notice.class);
        when(saved.getId()).thenReturn("notice-1");
        when(noticeRepository.save(any(Notice.class))).thenReturn(saved);

        // when
        String noticeId = noticeAdminService.createNotice(request);

        // then
        assertThat(noticeId).isEqualTo("notice-1");
        verify(noticeRepository).save(any(Notice.class));
    }
}
