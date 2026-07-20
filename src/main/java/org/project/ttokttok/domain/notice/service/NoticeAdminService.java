package org.project.ttokttok.domain.notice.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.notice.domain.Notice;
import org.project.ttokttok.domain.notice.repository.NoticeRepository;
import org.project.ttokttok.domain.notice.service.dto.request.CreateNoticeServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeAdminService {

    private final NoticeRepository noticeRepository;

    // 공지사항 생성 (운영자 전용)
    @Transactional
    public String createNotice(CreateNoticeServiceRequest request) {
        Notice notice = Notice.create(request.title(), request.content());

        return noticeRepository.save(notice)
                .getId();
    }
}
