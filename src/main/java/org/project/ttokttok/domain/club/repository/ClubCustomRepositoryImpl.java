package org.project.ttokttok.domain.club.repository;

import static org.project.ttokttok.domain.applyform.domain.QApplyForm.applyForm;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.applyform.repository.ApplyFormQueryConditions.openOn;
import static org.project.ttokttok.domain.applyform.repository.ApplyFormQueryConditions.upcomingOn;
import static org.project.ttokttok.domain.club.domain.QClub.club;
import static org.project.ttokttok.domain.clubmember.domain.QClubMember.clubMember;
import static org.project.ttokttok.domain.favorite.domain.QFavorite.favorite;

import java.util.HashSet;
import java.util.List;

import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.RecruitStatePolicy;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubSortType;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailAdminQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.PopularClubCandidate;
import org.project.ttokttok.global.policy.PopularityPolicy;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

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
                       MAX(CASE WHEN is_recruiting = true AND apply_start_date <= :today AND apply_end_date >= :today
                                THEN 1 ELSE 0 END) as is_recruiting,
                       MAX(CASE WHEN is_recruiting = true AND apply_start_date > :today
                                THEN 1 ELSE 0 END) as is_upcoming,
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
            )
            SELECT c.id, c.name, c.club_type, c.club_category, c.custom_category, c.summary, c.profile_img,
                   COALESCE(mc.cnt, 0) as member_count,
                   COALESCE(af_active.is_recruiting, 0) = 1 as recruiting,
                   COALESCE(af_active.is_upcoming, 0) = 1 as upcoming,
                   COALESCE(uf_user.bookmarked, 0) = 1 as bookmarked,
                   af_active.apply_end_date as apply_deadline,
                   COALESCE(fc.cnt, 0) as favorite_count,
                   c.view_count,
                   c.created_at
            FROM clubs c
            LEFT JOIN mc ON c.id = mc.club_id
            LEFT JOIN fc ON c.id = fc.club_id
            LEFT JOIN af_active ON c.id = af_active.club_id
            LEFT JOIN uf_user ON c.id = uf_user.club_id
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
                stateOf(activeForm),
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
    public List<PopularClubCandidate> findPopularClubCandidates(String userEmail) {
        var query = entityManager.createNativeQuery(POPULAR_CLUB_BASE_SQL);
        query.setParameter("userEmail", userEmail);
        query.setParameter("today", RecruitStatePolicy.today());

        List<Object[]> rows = query.getResultList();

        return rows.stream().map(this::toPopularClubCandidate).toList();
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

        // 관리자는 모집 스위치가 꺼진 활성 폼(마감, 되살린 직후)의 기간도 보고 고칠 수 있어야 한다.
        ApplyForm activeForm = queryFactory
                .selectFrom(applyForm)
                .leftJoin(applyForm.grades).fetchJoin()
                .where(applyForm.club.id.eq(clubId)
                        .and(applyForm.status.eq(ACTIVE)))
                .fetchOne();

        return new ClubDetailAdminQueryResponse(
                clubResult.get(0, String.class), clubResult.get(1, ClubType.class),
                clubResult.get(2, ClubCategory.class), clubResult.get(3, String.class),
                stateOf(activeForm), clubResult.get(4, String.class),
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

    /** 조회된 활성 폼이 없으면 마감이다. */
    private RecruitState stateOf(ApplyForm form) {
        return form != null ? form.recruitState() : RecruitState.CLOSED;
    }

    /** {@link #POPULAR_CLUB_BASE_SQL} 의 한 행을 후보로 바꾼다. 컬럼 순서가 SELECT 와 같아야 한다. */
    private PopularClubCandidate toPopularClubCandidate(Object[] row) {
        ClubCardQueryResponse card = new ClubCardQueryResponse(
                (String) row[0], (String) row[1], ClubType.valueOf((String) row[2]),
                ClubCategory.valueOf((String) row[3]), (String) row[4], (String) row[5],
                (String) row[6], ((Number) row[7]).intValue(), (Boolean) row[8], (Boolean) row[9],
                (Boolean) row[10], row[11] != null ? ((java.sql.Date) row[11]).toLocalDate() : null
        );

        return new PopularClubCandidate(
                card,
                ((Number) row[12]).longValue(),
                ((Number) row[13]).longValue(),
                ((java.sql.Timestamp) row[14]).toLocalDateTime()
        );
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
                        new CaseBuilder().when(openOn(applyForm, RecruitStatePolicy.today())).then(true).otherwise(false),
                        new CaseBuilder().when(upcomingOn(applyForm, RecruitStatePolicy.today())).then(true).otherwise(false),
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
                "({0}) * " + PopularityPolicy.MEMBER_WEIGHT + " + ({1}) * " + PopularityPolicy.FAVORITE_WEIGHT
                        + " + ({2}) * " + PopularityPolicy.VIEW_WEIGHT,
                memberCountSubQuery, favoriteCountSubQuery, club.viewCount);
    }

    /**
     * 정렬 조건에 따른 OrderBy 절을 적용합니다.
     */
    private void applySorting(JPAQuery<ClubCardQueryResponse> query, String sort) {
        switch (ClubSortType.from(sort)) {
            case POPULAR -> query.orderBy(getPopularityScoreExpression().desc(), club.id.desc());
            case MEMBER_COUNT -> {
                JPQLQuery<Long> memberCountSubQuery = JPAExpressions.select(clubMember.count()).from(clubMember)
                        .where(clubMember.club.id.eq(club.id));
                query.orderBy(Expressions.numberTemplate(Long.class, "({0})", memberCountSubQuery).desc(),
                        club.id.desc());
            }
            case LATEST -> query.orderBy(club.id.desc(), club.createdAt.desc());
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
                .where(applyForm.club.id.eq(club.id).and(openOn(applyForm, RecruitStatePolicy.today()))).exists();
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
