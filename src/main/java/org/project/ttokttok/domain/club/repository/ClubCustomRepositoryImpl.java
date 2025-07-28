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
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailAdminQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.project.ttokttok.domain.clubMember.domain.QClubMember;
import org.project.ttokttok.domain.favorite.domain.QFavorite;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        // 기본 Club 정보 조회
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

        // ApplyForm 정보 조회 (grades도 함께 fetch)
        ApplyForm activeForm = queryFactory
                .selectFrom(applyForm)
                .leftJoin(applyForm.grades).fetchJoin()
                .where(applyForm.club.id.eq(clubId)
                        .and(applyForm.status.eq(ACTIVE)))
                .fetchOne();

        return new ClubDetailQueryResponse(
                clubResult.get(0, String.class),           // name
                clubResult.get(1, ClubType.class),         // clubType
                clubResult.get(2, ClubCategory.class),     // clubCategory
                clubResult.get(3, String.class),           // customCategory
                Boolean.TRUE.equals(clubResult.get(4, Boolean.class)), // bookmarked
                activeForm != null, // ApplyForm이 존재하면 모집중
                clubResult.get(5, String.class),           // summary
                clubResult.get(6, String.class),           // profileImageUrl
                clubResult.get(7, Integer.class) != null ? clubResult.get(7, Integer.class) : 0, // clubMemberCount
                activeForm != null ? activeForm.getApplyStartDate() : null,    // ✅ ApplyForm 기준
                activeForm != null ? activeForm.getApplyEndDate() : null,      // ✅ ApplyForm 기준
                activeForm != null ? activeForm.getGrades() : new HashSet<>(), // ✅ ApplyForm 기준
                activeForm != null ? activeForm.getMaxApplyCount() : 0,        // ✅ ApplyForm 기준
                clubResult.get(8, String.class)            // content
        );
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
        return JPAExpressions.select(clubMember.count().coalesce(0L).intValue())
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
                        // ApplyForm이 ACTIVE 상태인지 확인
                        JPAExpressions.select(applyForm.count().gt(0))
                                .from(applyForm)
                                .where(applyForm.club.id.eq(club.id)
                                        .and(applyForm.status.eq(ACTIVE))),
                        bookmarkedSubQuery
                ))
                .from(club)
                .where(
                        categoryEq(category),
                        typeEq(type),
                        // recruitingEq(recruiting), - ApplyForm에서 관리하므로 제거
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
                            club.customCategory, club.summary, club.profileImageUrl)
                    .orderBy(popularityScore.desc(), club.id.desc());
        } else if ("member_count".equals(sort)) {
            query.leftJoin(club.clubMembers, clubMember)
                    .groupBy(club.id, club.name, club.clubType, club.clubCategory,
                            club.customCategory, club.summary, club.profileImageUrl)
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
        // recruiting은 이제 ApplyForm에서 관리되므로 항상 null 반환
        return null;
    }

    private BooleanExpression gradesEq(List<ApplicableGrade> grades) {
        if (grades == null || grades.isEmpty()) {
            return null;
        }

        // ApplyForm이 있고 해당 grades 중 하나라도 포함하는 동아리 찾기
        return JPAExpressions.selectOne()
                .from(applyForm)
                .where(applyForm.club.id.eq(club.id)
                        .and(applyForm.status.eq(ACTIVE))
                        .and(applyForm.grades.any().in(grades)))
                .exists();
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
                        // ApplyForm이 ACTIVE 상태인지 확인
                        JPAExpressions.select(applyForm.count().gt(0))
                                .from(applyForm)
                                .where(applyForm.club.id.eq(club.id)
                                        .and(applyForm.status.eq(ACTIVE))),
                        bookmarkedSubQuery
                ))
                .from(club)
                .where(
                        // club.recruiting.isTrue(), - ApplyForm에서 관리하므로 제거
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
                        // ApplyForm이 ACTIVE 상태인지 확인
                        JPAExpressions.select(applyForm.count().gt(0))
                                .from(applyForm)
                                .where(applyForm.club.id.eq(club.id)
                                        .and(applyForm.status.eq(ACTIVE))),
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
                        // ApplyForm이 ACTIVE 상태인지 확인
                        JPAExpressions.select(applyForm.count().gt(0))
                                .from(applyForm)
                                .where(applyForm.club.id.eq(club.id)
                                        .and(applyForm.status.eq(ACTIVE))),
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

    //TODO: 소개글 조회 자체를 추후 수정 필요
    @Override
    public ClubDetailAdminQueryResponse getAdminClubIntro(String clubId) {
        var clubResult = queryFactory
                .select(
                        club.name,
                        club.clubType,
                        club.clubCategory,
                        club.customCategory,
                        club.summary,
                        club.profileImageUrl,
                        getClubMemberCount(clubId),
                        club.clubUniv,  // clubUniv 필드 추가
                        club.content
                )
                .from(club)
                .where(club.id.eq(clubId))
                .fetchOne();

        if (clubResult == null) {
            return null;
        }

        // ApplyForm 정보 조회
        ApplyForm activeForm = queryFactory
                .selectFrom(applyForm)
                .leftJoin(applyForm.grades).fetchJoin()
                .where(applyForm.club.id.eq(clubId)
                        .and(applyForm.status.eq(ACTIVE)))
                .fetchOne();

        return new ClubDetailAdminQueryResponse(
                clubResult.get(0, String.class),           // name
                clubResult.get(1, ClubType.class),         // clubType
                clubResult.get(2, ClubCategory.class),     // clubCategory
                clubResult.get(3, String.class),           // customCategory
                activeForm != null, // ApplyForm이 존재하면 모집중 (recruiting)
                clubResult.get(4, String.class),           // summary
                clubResult.get(5, String.class),           // profileImageUrl
                clubResult.get(6, Integer.class) != null ? clubResult.get(6, Integer.class) : 0, // clubMemberCount
                clubResult.get(7, ClubUniv.class),         // clubUniv
                activeForm != null ? activeForm.getApplyStartDate() : null,    // applyStartDate
                activeForm != null ? activeForm.getApplyEndDate() : null,      // applyDeadLine
                activeForm != null ? activeForm.getGrades() : new HashSet<>(), // grades
                activeForm != null ? activeForm.getMaxApplyCount() : 0,        // maxApplyCount
                clubResult.get(8, String.class)            // content
        );
    }
}
