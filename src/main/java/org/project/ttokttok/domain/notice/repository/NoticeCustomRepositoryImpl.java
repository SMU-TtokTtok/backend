package org.project.ttokttok.domain.notice.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.notice.domain.Notice;
import org.project.ttokttok.domain.notice.repository.dto.NoticePageQueryResponse;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.project.ttokttok.domain.notice.domain.QNotice.notice;

@Repository
@RequiredArgsConstructor
public class NoticeCustomRepositoryImpl implements NoticeCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public NoticePageQueryResponse searchNotices(int page, int size, String keyword) {
        BooleanExpression keywordCondition = titleContains(keyword);

        List<Notice> content = queryFactory
                .selectFrom(notice)
                .where(keywordCondition)
                .orderBy(notice.createdAt.desc(), notice.id.desc())
                .offset((long) size * (page - 1))
                .limit(size)
                .fetch();

        Long totalCount = queryFactory
                .select(notice.count())
                .from(notice)
                .where(keywordCondition)
                .fetchOne();

        return NoticePageQueryResponse.builder()
                .content(content)
                .totalCount(totalCount == null ? 0L : totalCount)
                .build();
    }

    // keyword가 있으면 제목 부분일치 조건을, 없으면 null(조건 없음)을 반환한다.
    private BooleanExpression titleContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return notice.title.contains(keyword);
    }
}
