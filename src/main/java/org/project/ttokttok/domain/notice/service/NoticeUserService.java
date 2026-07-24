package org.project.ttokttok.domain.notice.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeDetailResponse;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeListResponse;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeListResponse.NoticeSummary;
import org.project.ttokttok.domain.notice.domain.Notice;
import org.project.ttokttok.domain.notice.exception.NoticeNotFoundException;
import org.project.ttokttok.domain.notice.repository.NoticeRepository;
import org.project.ttokttok.domain.notice.repository.dto.NoticePageQueryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeUserService {

    // 공개 엔드포인트이므로 과도한 size 요청(대량 조회/DoS)과 음수/0 page·size로 인한 오류를 방지한다.
    private static final int MIN_PAGE = 1;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final NoticeRepository noticeRepository;

    // 공지사항 목록 조회 (페이지 번호 기반 + 제목 검색, 비로그인 공개)
    @Transactional(readOnly = true)
    public NoticeListResponse getNotices(int page, int size, String keyword) {
        int safePage = Math.max(page, MIN_PAGE);
        int safeSize = Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);

        NoticePageQueryResponse queryResponse = noticeRepository.searchNotices(safePage, safeSize, keyword);

        int totalPage = (int) Math.ceil((double) queryResponse.totalCount() / safeSize);

        List<NoticeSummary> summaries = queryResponse.content().stream()
                .map(NoticeSummary::from)
                .toList();

        return NoticeListResponse.of(safePage, totalPage, (int) queryResponse.totalCount(), summaries);
    }

    // 공지사항 상세 조회 (조회 시 조회수 원자적 증가, 비로그인 공개)
    @Transactional
    public NoticeDetailResponse getNoticeDetail(String noticeId) {
        int updated = noticeRepository.increaseViewCount(noticeId);
        if (updated == 0) {
            throw new NoticeNotFoundException();
        }

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);

        return NoticeDetailResponse.from(notice);
    }
}
