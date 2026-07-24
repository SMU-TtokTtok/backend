package org.project.ttokttok.domain.notice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.notice.domain.Notice;
import org.project.ttokttok.domain.notice.repository.NoticeRepository;
import org.project.ttokttok.domain.notice.service.dto.request.CreateNoticeServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeAdminService {

    private final NoticeRepository noticeRepository;

    // 공지사항 생성 (운영자 전용)
    @Transactional
    public String createNotice(CreateNoticeServiceRequest request) {
        Notice notice = Notice.create(request.title(), request.content(), request.username());

        String noticeId = noticeRepository.save(notice)
                .getId();

        // 전역 공지 작성은 책임 추적을 위해 감사 로그를 남긴다.
        log.info("공지사항 생성: noticeId={}, createdBy={}", noticeId, request.username());

        return noticeId;
    }
}
