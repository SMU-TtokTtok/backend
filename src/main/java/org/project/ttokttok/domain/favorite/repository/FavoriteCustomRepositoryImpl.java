package org.project.ttokttok.domain.favorite.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.project.ttokttok.domain.favorite.repository.FavoriteCustomRepository;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteListServiceRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.project.ttokttok.domain.club.domain.QClub.club;
import static org.project.ttokttok.domain.favorite.domain.QFavorite.favorite;
import static org.project.ttokttok.domain.clubMember.domain.QClubMember.clubMember;

@RequiredArgsConstructor
public class FavoriteCustomRepositoryImpl implements FavoriteCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Favorite> findFavoritesByRequest(FavoriteListServiceRequest request) {
               // 다음 페이지 확인을 위해 요청된 size보다 1개 더 조회
        int fetchSize = request.size() + 1;
        JPAQuery<Favorite> query = queryFactory
                .selectFrom(favorite)
                .join(favorite.club, club).fetchJoin()
                .leftJoin(club.admin).fetchJoin()
                .where(favorite.user.email.eq(request.userEmail()));
        // 커서 기반 조건 추가
        BooleanExpression cursorCondition = getCursorCondition(request.cursor(), request.sort());
        if (cursorCondition != null) {
            query.where(cursorCondition);
        }
        // 정렬 조건 적용
        applyOrderBy(query, request.sort());
        return query.limit(fetchSize).fetch();
    }

    private void applyOrderBy(JPAQuery<Favorite> query, String sort) {
        switch (sort) {
            case "member_count" -> query.orderBy(club.clubMembers.size().desc(), favorite.createdAt.desc());
            case "popular" -> {
                // 'popular'는 서비스 계층에서 처리하므로 여기서는 기본 정렬(최신순) 적용
                query.orderBy(favorite.createdAt.desc());
            }
            default -> // "latest"
                    query.orderBy(favorite.createdAt.desc());
        }
    }

    private BooleanExpression getCursorCondition(String cursorId, String sort) {
        if (cursorId == null) {
            return null; // 첫 페이지 조회
        }

        // 커서가 되는 Favorite 엔티티 조회
        Favorite cursorFavorite = queryFactory
                .selectFrom(favorite)
                .join(favorite.club, club).fetchJoin()
                .where(favorite.id.eq(cursorId))
                .fetchOne();

        if (cursorFavorite == null) {
            return null; // 잘못된 커서
        }

        return switch (sort) {
            case "member_count" -> {
                int cursorMemberCount = cursorFavorite.getClub().getClubMembers().size();
                yield club.clubMembers.size().lt(cursorMemberCount)
                        .or(club.clubMembers.size().eq(cursorMemberCount)
                                .and(favorite.createdAt.lt(cursorFavorite.getCreatedAt())));
            }
            // 'popular'는 커서 기반 조회를 지원하지 않음
            default -> // "latest"
                favorite.createdAt.lt(cursorFavorite.getCreatedAt());
        };
    }
}
