package org.project.ttokttok.domain.clubboard.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.project.ttokttok.domain.clubboard.domain.QClubBoard.clubBoard;
import static org.project.ttokttok.domain.club.domain.QClub.club;

@Repository
@RequiredArgsConstructor
public class ClubBoardCustomRepositoryImpl implements ClubBoardCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ClubBoard> findBoardsByClubIdWithCursor(String clubId, int size, String cursor) {
        // 다음 페이지 확인을 위해 요청된 size보다 1개 더 조회
        int fetchSize = size + 1;
        
        JPAQuery<ClubBoard> query = queryFactory
                .selectFrom(clubBoard)
                .join(clubBoard.club, club).fetchJoin()
                .where(clubBoard.club.id.eq(clubId))
                .orderBy(clubBoard.createdAt.desc(), clubBoard.id.desc());

        // 커서 조건 추가
        BooleanExpression cursorCondition = getCursorCondition(cursor);
        if (cursorCondition != null) {
            query.where(cursorCondition);
        }

        return query.limit(fetchSize).fetch();
    }

    /**
     * 커서 조건을 생성합니다.
     * 커서가 있는 경우 해당 게시글보다 이전에 작성된 게시글들을 조회합니다.
     */
    private BooleanExpression getCursorCondition(String cursor) {
        if (cursor == null) {
            return null; // 첫 페이지 조회
        }

        // 커서가 되는 ClubBoard 엔티티 조회
        ClubBoard cursorBoard = queryFactory
                .selectFrom(clubBoard)
                .where(clubBoard.id.eq(cursor))
                .fetchOne();

        if (cursorBoard == null) {
            return null; // 잘못된 커서
        }

        LocalDateTime cursorCreatedAt = cursorBoard.getCreatedAt();
        
        // 생성시간이 더 이전이거나, 같은 시간이면 ID가 더 작은 것
        return clubBoard.createdAt.lt(cursorCreatedAt)
                .or(clubBoard.createdAt.eq(cursorCreatedAt)
                        .and(clubBoard.id.lt(cursor)));
    }
} 