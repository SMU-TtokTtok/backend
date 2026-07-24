package org.project.ttokttok.domain.notice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.notice.domain.Notice;
import org.project.ttokttok.domain.notice.repository.dto.NoticePageQueryResponse;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeRepositoryTest implements RepositoryTestSupport {

    @Autowired
    private NoticeRepository noticeRepository;

    @BeforeEach
    void setUp() {
        noticeRepository.save(Notice.create("공지 A 안내", "내용 A", "ttok_operator"));
        noticeRepository.save(Notice.create("공지 B 안내", "내용 B", "ttok_operator"));
        noticeRepository.save(Notice.create("이벤트 C", "내용 C", "ttok_operator"));
    }

    @Test
    @DisplayName("keyword 없이 조회하면 페이지 크기만큼 반환하고 전체 개수를 함께 반환한다.")
    void searchNoticesWithoutKeyword() {
        // when
        NoticePageQueryResponse response = noticeRepository.searchNotices(1, 2, null);

        // then
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.content()).hasSize(2);
    }

    @Test
    @DisplayName("keyword로 제목을 부분 검색하면 일치하는 공지만 반환한다.")
    void searchNoticesWithKeyword() {
        // when
        NoticePageQueryResponse response = noticeRepository.searchNotices(1, 10, "공지");

        // then
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.content()).allMatch(notice -> notice.getTitle().contains("공지"));
    }

    @Test
    @DisplayName("두 번째 페이지를 조회하면 offset이 적용되어 나머지 공지를 반환한다.")
    void searchNoticesSecondPage() {
        // when
        NoticePageQueryResponse response = noticeRepository.searchNotices(2, 2, null);

        // then
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    @DisplayName("increaseViewCount는 조회수를 DB에서 원자적으로 1 증가시킨다.")
    void increaseViewCount() {
        // given
        Notice saved = noticeRepository.save(Notice.create("조회수 테스트", "내용", "ttok_operator"));

        // when
        int updated = noticeRepository.increaseViewCount(saved.getId());
        Notice reloaded = noticeRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(updated).isEqualTo(1);
        assertThat(reloaded.getViewCount()).isEqualTo(1);
    }
}
