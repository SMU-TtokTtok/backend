package org.project.ttokttok.domain.club.repository;

import com.querydsl.core.types.OrderSpecifier;
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
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.project.ttokttok.domain.clubMember.domain.QClubMember;
import org.project.ttokttok.domain.favorite.domain.QFavorite;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Override
    public ClubDetailQueryResponse getClubIntroduction(String clubId, String email) {
        return queryFactory
                .select(Projections.constructor(ClubDetailQueryResponse.class,
                        club.name,
                        club.clubType,
                        club.clubCategory,
                        club.customCategory,
                        isFavorite(clubId, email),
                        club.recruiting,
                        club.summary,
                        club.profileImageUrl,
                        getClubMemberCount(clubId),
                        applyForm.applyStartDate,
                        applyForm.applyEndDate,
                        getGrades(clubId),
                        applyForm.maxApplyCount,
                        club.content
                ))
                .from(club)
                .leftJoin(applyForm).on(
                        applyForm.club.id.eq(clubId),
                        applyForm.status.stringValue().eq(ACTIVE.getStatus())
                )
                .where(club.id.eq(clubId))
                .fetchOne();
    }

    private JPQLQuery<ApplicableGrade> getGrades(String clubId) {
        return JPAExpressions.select(applyForm.grades.any())
                .from(applyForm)
                .where(applyForm.club.id.eq(clubId));
    }

    private BooleanExpression isFavorite(String clubId, String email) {
        if (email == null) {
            return Expressions.asBoolean(false);
        }
        return JPAExpressions.selectOne()
                .from(favorite)
                .where(favorite.user.email.eq(email)
                        .and(favorite.club.id.eq(clubId)))
                .exists();
    }

    private JPQLQuery<Integer> getClubMemberCount(String clubId) {
        return JPAExpressions.select(clubMember.count().intValue())
                .from(clubMember)
                .where(clubMember.club.id.eq(clubId));
    }

    @Override
    public List<ClubCardQueryResponse> getClubList(
            ClubCategory category,
            ClubType type,
            Boolean recruiting,
            List<ApplicableGrade> grades,
            int size,
            String cursor,
            String sort,
            String userEmail) {

        JPQLQuery<Boolean> bookmarkedSubQuery = (userEmail == null) ?
                JPAExpressions.select(Expressions.constant(false)) :
                JPAExpressions.select(favorite.count().gt(0))
                        .from(favorite)
                        .where(
                                favorite.club.id.eq(club.id),
                                favorite.user.email.eq(userEmail)
                        );

        JPAQuery<ClubCardQueryResponse> query = queryFactory
                .select(Projections.constructor(ClubCardQueryResponse.class,
                        club.id,
                        club.name,
                        club.clubType,
                        club.clubCategory,
                        club.customCategory,
                        club.summary,
                        club.profileImageUrl,
                        JPAExpressions.select(clubMember.count().intValue())
                                .from(clubMember)
                                .where(clubMember.club.id.eq(club.id)),
                        club.recruiting,
                        bookmarkedSubQuery
                ))
                .from(club)
                .where(
                        categoryEq(category),
                        typeEq(type),
                        recruitingEq(recruiting),
                        gradesEq(grades),
                        cursorCondition(cursor, sort)
                );

        if ("popular".equals(sort)) {
            QClubMember joinedClubMember = new QClubMember("joinedClubMember");
            QFavorite joinedFavorite = new QFavorite("joinedFavorite");
            NumberExpression<Double> popularityScore =
                    joinedClubMember.count().doubleValue().multiply(0.7)
                            .add(joinedFavorite.count().doubleValue().multiply(0.3));
            query.leftJoin(club.clubMembers, joinedClubMember)
                    .leftJoin(joinedFavorite).on(joinedFavorite.club.eq(club))
                    .groupBy(club.id, club.name, club.clubType, club.clubCategory,
                            club.customCategory, club.summary, club.profileImageUrl, club.recruiting)
                    .orderBy(popularityScore.desc(), club.id.desc());
        } else if ("member_count".equals(sort)) {
            query.leftJoin(club.clubMembers, clubMember)
                    .groupBy(club.id, club.name, club.clubType, club.clubCategory,
                            club.customCategory, club.summary, club.profileImageUrl, club.recruiting)
                    .orderBy(clubMember.count().desc(), club.id.desc());
        } else {
            query.orderBy(club.createdAt.desc(), club.id.desc());
        }

        query.limit(size + 1);
        return query.fetch();
    }

    private BooleanExpression categoryEq(ClubCategory category) {
        return category != null ? club.clubCategory.eq(category) : null;
    }

    private BooleanExpression typeEq(ClubType type) {
        return type != null ? club.clubType.eq(type) : null;
    }

    private BooleanExpression recruitingEq(Boolean recruiting) {
        return recruiting != null ? club.recruiting.eq(recruiting) : null;
    }

    private BooleanExpression gradesEq(List<ApplicableGrade> grades) {
        if (grades == null || grades.isEmpty()) {
            return null;
        }

        // JSONB 컬럼을 텍스트로 캐스팅하고, 각 학년 이름이 포함되어 있는지 OR 조건으로 묶어서 확인
        BooleanExpression combinedExpression = null;
        for (ApplicableGrade grade : grades) {
            // "FIRST_GRADE", "SECOND_GRADE" 와 같이 큰따옴표로 감싸진 문자열을 찾도록 조건 구성
            BooleanExpression likeExpression = Expressions.stringTemplate("cast({0} as text)", club.targetGrades)
                    .containsIgnoreCase("\"" + grade.name() + "\"");

            if (combinedExpression == null) {
                combinedExpression = likeExpression;
            } else {
                combinedExpression = combinedExpression.or(likeExpression);
            }
        }
        return combinedExpression;
    }

    private BooleanExpression cursorCondition(String cursor, String sort) {
        if (cursor == null) {
            return null;
        }
        String currentSort = (sort == null) ? "latest" : sort;
        switch (currentSort) {
            case "latest":
                try {
                    return club.createdAt.lt(LocalDateTime.parse(cursor));
                } catch (Exception e) {
                    return club.id.lt(cursor);
                }
            case "popular":
            case "member_count":
                return club.id.lt(cursor);
            default:
                return club.id.lt(cursor);
        }
    }

    @Override
    public List<ClubCardQueryResponse> getAllPopularClubs(String userEmail, double minScore) {
        JPQLQuery<Long> memberCountSubQuery = JPAExpressions
                .select(clubMember.count())
                .from(clubMember)
                .where(clubMember.club.id.eq(club.id));
        JPQLQuery<Long> favoriteCountSubQuery = JPAExpressions
                .select(favorite.count())
                .from(favorite)
                .where(favorite.club.id.eq(club.id));
        NumberExpression<Double> popularityScore = Expressions.numberTemplate(Double.class,
                "({0}) * 0.7 + ({1}) * 0.3",
                memberCountSubQuery, favoriteCountSubQuery);
        JPQLQuery<Boolean> bookmarkedSubQuery = (userEmail == null) ?
                JPAExpressions.select(Expressions.constant(false)) :
                JPAExpressions.select(favorite.count().gt(0))
                        .from(favorite)
                        .where(
                                favorite.club.id.eq(club.id),
                                favorite.user.email.eq(userEmail)
                        );
        return queryFactory
                .select(Projections.constructor(ClubCardQueryResponse.class,
                        club.id,
                        club.name,
                        club.clubType,
                        club.clubCategory,
                        club.customCategory,
                        club.summary,
                        club.profileImageUrl,
                        Expressions.numberTemplate(Integer.class, "({0})", memberCountSubQuery),
                        club.recruiting,
                        bookmarkedSubQuery
                ))
                .from(club)
                .where(
                        club.recruiting.isTrue(),
                        popularityScore.goe(minScore)
                )
                .orderBy(popularityScore.desc(), club.id.desc())
                .fetch();
    }

    @Override
    public List<ClubCardQueryResponse> getPopularClubsWithFilters(
            int size,
            String cursor,
            String sort,
            String userEmail,
            double minScore) {

        JPQLQuery<Long> memberCountSubQuery = JPAExpressions
                .select(clubMember.count())
                .from(clubMember)
                .where(clubMember.club.id.eq(club.id));
        JPQLQuery<Long> favoriteCountSubQuery = JPAExpressions
                .select(favorite.count())
                .from(favorite)
                .where(favorite.club.id.eq(club.id));
        NumberExpression<Double> popularityScore = Expressions.numberTemplate(Double.class,
                "({0}) * 0.7 + ({1}) * 0.3",
                memberCountSubQuery, favoriteCountSubQuery);
        JPQLQuery<Boolean> bookmarkedSubQuery = (userEmail == null) ?
                JPAExpressions.select(Expressions.constant(false)) :
                JPAExpressions.select(favorite.count().gt(0))
                        .from(favorite)
                        .where(
                                favorite.club.id.eq(club.id),
                                favorite.user.email.eq(userEmail)
                        );
        JPAQuery<ClubCardQueryResponse> query = queryFactory
                .select(Projections.constructor(ClubCardQueryResponse.class,
                        club.id,
                        club.name,
                        club.clubType,
                        club.clubCategory,
                        club.customCategory,
                        club.summary,
                        club.profileImageUrl,
                        Expressions.numberTemplate(Integer.class, "({0})", memberCountSubQuery),
                        club.recruiting,
                        bookmarkedSubQuery
                ))
                .from(club)
                .where(
                        popularityScore.goe(minScore),
                        cursorCondition(cursor, sort)
                );

        if ("popular".equals(sort)) {
            query.orderBy(popularityScore.desc(), club.id.desc());
        } else if ("member_count".equals(sort)) {
            query.orderBy(Expressions.numberTemplate(Long.class, "({0})", memberCountSubQuery).desc(), club.id.desc());
        } else { // "latest"
            query.orderBy(club.createdAt.desc(), club.id.desc());
        }

        query.limit(size + 1);
        return query.fetch();
    }

    @Override
    public List<ClubCardQueryResponse> searchByKeyword(String keyword, int size, String cursor, String sort, String userEmail) {
        JPQLQuery<Integer> memberCountSubQuery = JPAExpressions
                .select(clubMember.count().intValue())
                .from(clubMember)
                .where(clubMember.club.id.eq(club.id));
        JPQLQuery<Boolean> bookmarkedSubQuery = (userEmail == null) ?
                JPAExpressions.select(Expressions.constant(false)) :
                JPAExpressions.select(favorite.count().gt(0))
                        .from(favorite)
                        .where(favorite.club.id.eq(club.id).and(favorite.user.email.eq(userEmail)));
        JPAQuery<ClubCardQueryResponse> query = queryFactory
                .select(Projections.constructor(ClubCardQueryResponse.class,
                        club.id,
                        club.name,
                        club.clubType,
                        club.clubCategory,
                        club.customCategory,
                        club.summary,
                        club.profileImageUrl,
                        memberCountSubQuery,
                        club.recruiting,
                        bookmarkedSubQuery
                ))
                .from(club)
                .where(
                        club.name.containsIgnoreCase(keyword)
                                .or(club.summary.containsIgnoreCase(keyword))
                                .or(club.content.containsIgnoreCase(keyword)),
                        cursorCondition(cursor, sort)
                );

        String currentSort = (sort == null) ? "latest" : sort;
        switch (currentSort) {
            case "popular":
                JPQLQuery<Long> favoriteCountSubQuery = JPAExpressions
                        .select(favorite.count())
                        .from(favorite)
                        .where(favorite.club.id.eq(club.id));
                NumberExpression<Double> popularityScore = Expressions.numberTemplate(Double.class,
                        "({0}) * 0.7 + ({1}) * 0.3",
                        Expressions.numberTemplate(Long.class, "({0})", memberCountSubQuery),
                        favoriteCountSubQuery);
                query.orderBy(popularityScore.desc(), club.id.desc());
                break;
            case "member_count":
                NumberExpression<Integer> memberCountExpression = Expressions.numberTemplate(Integer.class, "({0})", memberCountSubQuery);
                query.orderBy(memberCountExpression.desc(), club.id.desc());
                break;
            case "latest":
            default:
                query.orderBy(club.createdAt.desc(), club.id.desc());
                break;
        }

        query.limit(size + 1);
        return query.fetch();
    }

    @Override
    public long countByKeyword(String keyword) {
        return queryFactory
                .select(club.count())
                .from(club)
                .where(
                        club.name.containsIgnoreCase(keyword)
                                .or(club.summary.containsIgnoreCase(keyword))
                                .or(club.content.containsIgnoreCase(keyword))
                )
                .fetchOne();
    }
}
