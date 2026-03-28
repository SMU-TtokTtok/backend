package org.project.ttokttok.domain.club.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailAdminQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;

import static org.project.ttokttok.domain.applyform.domain.QApplyForm.applyForm;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.club.domain.QClub.club;
import static org.project.ttokttok.domain.clubMember.domain.QClubMember.clubMember;
import static org.project.ttokttok.domain.favorite.domain.QFavorite.favorite;

@Repository
@RequiredArgsConstructor
public class ClubCustomRepositoryImpl implements ClubCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    private static final String POPULAR_CLUB_BASE_SQL = """
            WITH mc AS (
                SELECT club_id, COUNT(*) as cnt FROM club_members GROUP BY club_id
            ),
            fc AS (
                SELECT club_id, COUNT(*) as cnt FROM user_favorites GROUP BY club_id
            ),
            af_active AS (
                SELECT club_id, 
                       MAX(CASE WHEN is_recruiting = true THEN 1 ELSE 0 END) as is_recruiting,
                       MAX(apply_end_date) as apply_end_date
                FROM applyforms 
                WHERE status = 'ACTIVE'
                GROUP BY club_id
            ),
            uf_user AS (
                SELECT f.club_id, 1 as bookmarked 
                FROM user_favorites f
                JOIN users u ON f.user_id = u.id
                WHERE u.email = :userEmail
            ),
            scored_clubs AS (
                SELECT c.id, c.name, c.club_type, c.club_category, c.custom_category, c.summary, c.profile_img, c.view_count, c.created_at,
                       COALESCE(mc.cnt, 0) as member_count,
                       (COALESCE(mc.cnt, 0) * 0.7 + COALESCE(fc.cnt, 0) * 2.5 + c.view_count * 0.7) AS score,
                       COALESCE(af_active.is_recruiting, 0) = 1 as recruiting,
                       COALESCE(uf_user.bookmarked, 0) = 1 as bookmarked,
                       af_active.apply_end_date as apply_deadline
                FROM clubs c
                LEFT JOIN mc ON c.id = mc.club_id
                LEFT JOIN fc ON c.id = fc.club_id
                LEFT JOIN af_active ON c.id = af_active.club_id
                LEFT JOIN uf_user ON c.id = uf_user.club_id
            )
            SELECT id, name, club_type, club_category, custom_category, summary, profile_img,
                   member_count, recruiting, bookmarked, apply_deadline, score
            FROM scored_clubs
            WHERE score >= :minScore
            """;

    @Override
    public ClubDetailQueryResponse getClubIntroduction(String clubId, String email) {
        var clubResult = queryFactory
                .select(
                        club.name,
                        club.clubType,
                        club.clubCategory,
                        club.customCategory,
                        isFavorite(clubId, email),
                        club.summary,
                        club.profileImageUrl,
                        getClubMemberCount(clubId),
                        club.content
                )
                .from(club)
                .where(club.id.eq(clubId))
                .fetchOne();

        if (clubResult == null) {
            return null;
        }

        ApplyForm activeForm = queryFactory
                .selectFrom(applyForm)
                .leftJoin(applyForm.grades).fetchJoin()
                .where(applyForm.club.id.eq(clubId)
                        .and(applyForm.status.eq(ACTIVE))
                        .and(applyForm.isRecruiting.eq(true)))
                .fetchOne();

        return new ClubDetailQueryResponse(
                clubResult.get(0, String.class),
                clubResult.get(1, ClubType.class),
                clubResult.get(2, ClubCategory.class),
                clubResult.get(3, String.class),
                Boolean.TRUE.equals(clubResult.get(4, Boolean.class)),
                activeForm != null && activeForm.isRecruiting(),
                clubResult.get(5, String.class),
                clubResult.get(6, String.class),
                clubResult.get(7, Integer.class) != null ? clubResult.get(7, Integer.class) : 0,
                activeForm != null ? activeForm.getApplyStartDate() : null,
                activeForm != null ? activeForm.getApplyEndDate() : null,
                activeForm != null ? activeForm.getGrades() : new HashSet<>(),
                activeForm != null ? activeForm.getMaxApplyCount() : 0,
                clubResult.get(8, String.class)
        );
    }

    @Override
    public List<ClubCardQueryResponse> getClubList(
            ClubCategory category,
            ClubType type,
            ClubUniv clubUniv,
            Boolean recruiting,
            List<ApplicableGrade> grades,
            int size,
            String cursor,
            String sort,
            String userEmail) {

        JPAQuery<ClubCardQueryResponse> query = selectClubCard(userEmail)
                .where(
                        categoryEq(category),
                        typeEq(type),
                        clubUnivEq(clubUniv),
                        recruitingEq(recruiting),
                        gradesEq(grades),
                        cursorCondition(cursor)
                );

        applySorting(query, sort);
        query.limit(size + 1);

        return query.fetch();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ClubCardQueryResponse> getAllPopularClubs(String userEmail, double minScore) {
        String sql = POPULAR_CLUB_BASE_SQL + " ORDER BY score DESC, id DESC ";

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("userEmail", userEmail);
        query.setParameter("minScore", minScore);

        return mapToClubCardQueryResponse(query.getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ClubCardQueryResponse> getPopularClubsWithFilters(
            int size,
            String cursor,
            String sort,
            String userEmail,
            double minScore) {

        StringBuilder sqlBuilder = new StringBuilder(POPULAR_CLUB_BASE_SQL);
        if (cursor != null) {
            sqlBuilder.append(" AND id < :cursor ");
        }

        if ("popular".equals(sort)) {
            sqlBuilder.append(" ORDER BY score DESC, id DESC ");
        } else if ("member_count".equals(sort)) {
            sqlBuilder.append(" ORDER BY member_count DESC, id DESC ");
        } else {
            sqlBuilder.append(" ORDER BY id DESC, created_at DESC ");
        }

        sqlBuilder.append(" LIMIT :limit ");

        var query = entityManager.createNativeQuery(sqlBuilder.toString());
        query.setParameter("userEmail", userEmail);
        query.setParameter("minScore", minScore);
        query.setParameter("limit", size + 1);
        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }

        return mapToClubCardQueryResponse(query.getResultList());
    }

    @Override
    public List<ClubCardQueryResponse> searchByKeyword(String keyword, int size, String cursor, String sort,
                                                       String userEmail) {
        JPAQuery<ClubCardQueryResponse> query = selectClubCard(userEmail)
                .where(
                        club.name.containsIgnoreCase(keyword),
                        cursorCondition(cursor)
                );

        applySorting(query, sort);
        query.limit(size + 1);

        return query.fetch();
    }

    @Override
    public long countByKeyword(String keyword) {
        return queryFactory
                .select(club.count())
                .from(club)
                .where(club.name.containsIgnoreCase(keyword))
                .fetchOne();
    }

    @Override
    public ClubDetailAdminQueryResponse getAdminClubIntro(String clubId) {
        var clubResult = queryFactory
                .select(
                        club.name, club.clubType, club.clubCategory, club.customCategory,
                        club.summary, club.profileImageUrl, getClubMemberCount(clubId),
                        club.clubUniv, club.content
                )
                .from(club)
                .where(club.id.eq(clubId))
                .fetchOne();

        if (clubResult == null) {
            return null;
        }

        ApplyForm activeForm = queryFactory
                .selectFrom(applyForm)
                .leftJoin(applyForm.grades).fetchJoin()
                .where(applyForm.club.id.eq(clubId)
                        .and(applyForm.status.eq(ACTIVE))
                        .and(applyForm.isRecruiting.eq(true)))
                .fetchOne();

        return new ClubDetailAdminQueryResponse(
                clubResult.get(0, String.class), clubResult.get(1, ClubType.class),
                clubResult.get(2, ClubCategory.class), clubResult.get(3, String.class),
                activeForm != null && activeForm.isRecruiting(), clubResult.get(4, String.class),
                clubResult.get(5, String.class),
                clubResult.get(6, Integer.class) != null ? clubResult.get(6, Integer.class) : 0,
                clubResult.get(7, ClubUniv.class), activeForm != null ? activeForm.getApplyStartDate() : null,
                activeForm != null ? activeForm.getApplyEndDate() : null,
                activeForm != null ? activeForm.getGrades() : new HashSet<>(),
                activeForm != null ? activeForm.getMaxApplyCount() : 0,
                clubResult.get(8, String.class)
        );
    }

    // --- Private Helper Methods ---

    private List<ClubCardQueryResponse> mapToClubCardQueryResponse(List<Object[]> results) {
        return results.stream().map(row -> new ClubCardQueryResponse(
                (String) row[0], (String) row[1], ClubType.valueOf((String) row[2]),
                ClubCategory.valueOf((String) row[3]), (String) row[4], (String) row[5],
                (String) row[6], ((Number) row[7]).intValue(), (Boolean) row[8],
                (Boolean) row[9], row[10] != null ? ((java.sql.Date) row[10]).toLocalDate() : null
        )).toList();
    }

    /**
     * ClubCardQueryResponse를 반환하기 위한 기본 Select 및 Join 구성을 생성합니다.
     */
    private JPAQuery<ClubCardQueryResponse> selectClubCard(String userEmail) {
        JPQLQuery<Long> memberCountSubQuery = JPAExpressions
                .select(clubMember.count())
                .from(clubMember)
                .where(clubMember.club.id.eq(club.id));

        BooleanExpression isBookmarked = (userEmail != null) ? favorite.id.isNotNull() : Expressions.asBoolean(false);

        var query = queryFactory
                .select(Projections.constructor(ClubCardQueryResponse.class,
                        club.id, club.name, club.clubType, club.clubCategory, club.customCategory,
                        club.summary, club.profileImageUrl,
                        Expressions.numberTemplate(Integer.class, "({0})", memberCountSubQuery),
                        applyForm.isRecruiting.coalesce(false),
                        isBookmarked,
                        applyForm.applyEndDate
                ))
                .from(club)
                .leftJoin(applyForm).on(applyForm.club.id.eq(club.id).and(applyForm.status.eq(ACTIVE)));

        if (userEmail != null) {
            query.leftJoin(favorite).on(favorite.club.id.eq(club.id).and(favorite.user.email.eq(userEmail)));
        }

        return query;
    }

    /**
     * 인기도 점수 계산식을 반환합니다.
     */
    private NumberExpression<Double> getPopularityScoreExpression() {
        JPQLQuery<Long> memberCountSubQuery = JPAExpressions.select(clubMember.count()).from(clubMember)
                .where(clubMember.club.id.eq(club.id));
        JPQLQuery<Long> favoriteCountSubQuery = JPAExpressions.select(favorite.count()).from(favorite)
                .where(favorite.club.id.eq(club.id));

        return Expressions.numberTemplate(Double.class,
                "({0}) * 0.7 + ({1}) * 2.5 + ({2}) * 0.7",
                memberCountSubQuery, favoriteCountSubQuery, club.viewCount);
    }

    /**
     * 정렬 조건에 따른 OrderBy 절을 적용합니다.
     */
    private void applySorting(JPAQuery<ClubCardQueryResponse> query, String sort) {
        if ("popular".equals(sort)) {
            query.orderBy(getPopularityScoreExpression().desc(), club.id.desc());
        } else if ("member_count".equals(sort)) {
            JPQLQuery<Long> memberCountSubQuery = JPAExpressions.select(clubMember.count()).from(clubMember)
                    .where(clubMember.club.id.eq(club.id));
            query.orderBy(Expressions.numberTemplate(Long.class, "({0})", memberCountSubQuery).desc(), club.id.desc());
        } else {
            query.orderBy(club.id.desc(), club.createdAt.desc());
        }
    }

    private BooleanExpression categoryEq(ClubCategory category) {
        return category != null ? club.clubCategory.eq(category) : null;
    }

    private BooleanExpression typeEq(ClubType type) {
        return type != null ? club.clubType.eq(type) : null;
    }

    private BooleanExpression clubUnivEq(ClubUniv clubUniv) {
        return clubUniv != null ? club.clubUniv.eq(clubUniv) : null;
    }

    private BooleanExpression recruitingEq(Boolean recruiting) {
        if (recruiting == null) {
            return null;
        }
        BooleanExpression hasActive = JPAExpressions.selectOne().from(applyForm)
                .where(applyForm.club.id.eq(club.id).and(applyForm.status.eq(ACTIVE))
                        .and(applyForm.isRecruiting.eq(true))).exists();
        return recruiting ? hasActive : hasActive.not();
    }

    private BooleanExpression gradesEq(List<ApplicableGrade> grades) {
        if (grades == null || grades.isEmpty()) {
            return null;
        }
        return JPAExpressions.selectOne().from(applyForm)
                .where(applyForm.club.id.eq(club.id).and(applyForm.status.eq(ACTIVE))
                        .and(applyForm.grades.any().in(grades))).exists();
    }

    private BooleanExpression cursorCondition(String cursor) {
        return cursor != null ? club.id.lt(cursor) : null;
    }

    private BooleanExpression isFavorite(String clubId, String email) {
        if (email == null) {
            return Expressions.asBoolean(false);
        }
        return JPAExpressions.selectOne().from(favorite)
                .where(favorite.user.email.eq(email).and(favorite.club.id.eq(clubId))).exists();
    }

    private JPQLQuery<Integer> getClubMemberCount(String clubId) {
        return JPAExpressions.select(clubMember.count().coalesce(0L).intValue()).from(clubMember)
                .where(clubMember.club.id.eq(clubId));
    }
}
