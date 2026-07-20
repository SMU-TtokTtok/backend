package org.project.ttokttok.domain.notice.repository;

import org.project.ttokttok.domain.notice.repository.dto.NoticePageQueryResponse;

public interface NoticeCustomRepository {

    // 공지사항 목록을 페이지 번호(1-based) 기반으로 조회하며, keyword가 있으면 제목으로 필터링한다.
    NoticePageQueryResponse searchNotices(int page, int size, String keyword);
}
