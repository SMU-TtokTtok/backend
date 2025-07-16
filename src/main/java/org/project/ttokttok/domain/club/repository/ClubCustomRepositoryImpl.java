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
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.QClub;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.project.ttokttok.domain.clubMember.domain.QClubMember;
import org.project.ttokttok.domain.favorite.domain.QFavorite;
import org.project.ttokttok.domain.user.domain.QUser;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
                .leftJoin(favorite).on(
                        favorite.club.id.eq(clubId),
                        favorite.user.email.eq(email)
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

    // 무한스크롤용 동아리 목록 조회 -> 무한스크롤 방식으로 동아리 목록증 조회하는 핵심 메서드
    @Override
    public List<ClubCardQueryResponse> getClubList(
            // 필터링 조건들
            ClubCategory category,  // 카테고리 필터 (봉사, 예술, 문화 등)
            ClubType type,          // 동아리 분류 (중앙, 연합, 학과)
            Boolean recruiting,     // 모집 여부 (true/false)
            List<ApplicableGrade> grades,

            // 페이징 관련
            int size,               // 조회할 개수
            String cursor,          // 커서 (무한스크롤용)
            String sort,            // 정렬 방식 (latest, popular)

            // 사용자 관련
            String userEmail) {     // 즐겨찾기 확인용 사용자 이메일

        // 학년 필터링이 있을 때만 Native Query 사용
        if (grades != null && !grades.isEmpty()) {
            return getClubListWithNativeQuery(category, type, recruiting, grades, size, cursor, sort, userEmail);
        }

        JPAQuery<ClubCardQueryResponse> query = queryFactory
                .select(Projections.constructor(ClubCardQueryResponse.class,
                        club.id,                // 동아리 ID
                        club.name,              // 동아리 이름
                        club.clubType,          // 동아리 분류
                        club.clubCategory,      // 동아리 카테고리
                        club.customCategory,    // 커스텀 카테고리
                        club.summary,           // 한줄 소개
                        club.profileImageUrl,   // 프로필 이미지 URL

                        // 서브쿼리로 멤버 수 계산
                        JPAExpressions.select(clubMember.count().intValue())
                                .from(clubMember)
                                .where(clubMember.club.id.eq(club.id)),
                        club.recruiting,    // 모집 여부

                        // 서브쿼리로 즐겨찾기 여부 확인
                        JPAExpressions.select(favorite.count().gt(0))
                                .from(favorite)
                                .where(
                                        favorite.club.id.eq(club.id),
                                        favorite.user.email.eq(userEmail)
                                )
                ))
                .from(club)
                .where(
                        categoryEq(category),       // 카테고리 필터
                        typeEq(type),               // 분류 필터
                        recruitingEq(recruiting),   // 모집 여부 필터
                        gradesEq(grades),
                        cursorCondition(cursor, sort)     // 커서 조건 (무한스크롤)
                );

        // 정렬 방식에 따라 orderBy 설정
        if ("popular".equals(sort)) {
            // 멤버수와 즐겨찾기수를 JOIN으로 계산
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
            // 멤버수 기준 정렬
            query.leftJoin(club.clubMembers, clubMember)
                    .groupBy(club.id, club.name, club.clubType, club.clubCategory,
                            club.customCategory, club.summary, club.profileImageUrl, club.recruiting)
                    .orderBy(clubMember.count().desc(), club.id.desc());
        } else {
            // 최신순 정렬 (기본값)
            query.orderBy(club.createdAt.desc(), club.id.desc());
        }

        query.limit(size + 1);  // size + 1개 조회 (hashNext 판단용)

        return query.fetch();
    }

    // 조건별 필터링 메서드들 추가
    private BooleanExpression categoryEq(ClubCategory category) {
        return category != null ? club.clubCategory.eq(category) : null;
    }

    private BooleanExpression typeEq(ClubType type) {
        return type != null ? club.clubType.eq(type) : null;
    }

    private BooleanExpression recruitingEq(Boolean recruiting) {
        return recruiting != null ? club.recruiting.eq(recruiting) : null;
    }

    // 학년 필터링 메서드 추가
    private BooleanExpression gradesEq(List<ApplicableGrade> grades) {
        if (grades == null) {
            return null;
        }

        // PostgreSQL JSONB ?| 연산자 사용 (배열 요소 중 하나라도 포함되면 true)
        String[] gradeNames = grades.stream()
                .map(ApplicableGrade::name)
                .toArray(String[]::new);

        return Expressions.booleanTemplate(
                "{0} ?| {1}",
                club.targetGrades,
                gradeNames
        );
    }

    private BooleanExpression cursorCondition(String cursor, String sort) {
        if (cursor == null) {
            return null;
        }

        String currentSort = (sort == null) ? "latest" : sort;

        // 인기순 정렬일 때는 커서 기반 무한스크롤이 복잡하므로 
        // 현재는 최신순 정렬에서만 커서를 적용
//        if ("popular".equals(sort)) {
//            return null;  // 인기순에서는 커서 무시 (전체 조회)
//        }
        switch (currentSort) {
            case "latest":
                // 최신순 : 생성일 기준 커서 (더 정확한 페이징)
                try {
                    return club.createdAt.lt(LocalDateTime.parse(cursor));
                } catch (Exception e) {
                    // 파싱 실패 시 ID 기준으로 fallback
                    return club.id.lt(cursor);
                }

            case "popular":
            case "member_count":
                // 인기순/멤버순 : ID 기준 커서 (단순하지만 일단 1차적으로 이렇게 구현
                return club.id.lt(cursor);

            default:
                // 기본값 : ID 기준 커서
                return club.id.lt(cursor);
        }
    }

    // 복합 인기도 기준 동아리 조회 (부원수 x 0.7 + 즐겨찾기 수 x 0.3)
    @Override
    public List<ClubCardQueryResponse> getAllPopularClubs(String userEmail, double minScore) {
        String sql = """
        SELECT c.id, c.name, c.club_type, c.club_category, c.custom_category, 
               c.summary, c.profile_img,
               COALESCE((SELECT COUNT(*) FROM club_members cm WHERE cm.club_id = c.id), 0) as member_count,
               c.recruiting,
               COALESCE((SELECT COUNT(*) > 0 FROM user_favorites uf 
                        WHERE uf.club_id = c.id 
                        AND uf.user_id = (SELECT id FROM users WHERE email = ?)), false) as bookmarked
        FROM clubs c
        WHERE c.recruiting = true
        AND (
            COALESCE((SELECT COUNT(*) FROM club_members cm WHERE cm.club_id = c.id), 0) * 0.7 + 
            COALESCE((SELECT COUNT(*) FROM user_favorites uf WHERE uf.club_id = c.id), 0) * 0.3
        ) >= ?
        ORDER BY (
            COALESCE((SELECT COUNT(*) FROM club_members cm WHERE cm.club_id = c.id), 0) * 0.7 + 
            COALESCE((SELECT COUNT(*) FROM user_favorites uf WHERE uf.club_id = c.id), 0) * 0.3
        ) DESC, c.id DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, userEmail);
        query.setParameter(2, minScore);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new ClubCardQueryResponse(
                        (String) row[0],  // id
                        (String) row[1],  // name
                        ClubType.valueOf((String) row[2]),  // clubType
                        ClubCategory.valueOf((String) row[3]),  // clubCategory
                        (String) row[4],  // customCategory
                        (String) row[5],  // summary
                        (String) row[6],  // profileImageUrl
                        ((Number) row[7]).intValue(),  // memberCount
                        (Boolean) row[8],  // recruiting
                        (Boolean) row[9]   // bookmarked
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClubCardQueryResponse> getPopularClubsWithFilters(
            int size,
            String cursor,
            String sort,
            String userEmail,
            double minScore) {

        // 멤버 수와 즐겨찾기 수 계산을 위한 서브쿼리를 JPQLQuery 타입으로 정의
        JPQLQuery<Long> memberCountSubQuery = JPAExpressions
                .select(clubMember.count())
                .from(clubMember)
                .where(clubMember.club.id.eq(club.id));

        JPQLQuery<Long> favoriteCountSubQuery = JPAExpressions
                .select(favorite.count())
                .from(favorite)
                .where(favorite.club.id.eq(club.id));

        // Expressions.numberTemplate을 사용해 두 서브쿼리의 결과를 조합하여 인기도 점수 계산
        NumberExpression<Double> popularityScore = Expressions.numberTemplate(Double.class,
                "({0}) * 0.7 + ({1}) * 0.3",
                memberCountSubQuery, favoriteCountSubQuery);

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
                        // 즐겨찾기 여부
                        JPAExpressions.select(favorite.count().gt(0))
                                .from(favorite)
                                .where(
                                        favorite.club.id.eq(club.id),
                                        favorite.user.email.eq(userEmail)
                                )
                ))
                .from(club)
                .where(
                        // 위에서 생성한 인기도 점수 표현식을 where 절에서 사용
                        popularityScore.goe(minScore),
                        // 커서 조건 적용
                        cursorCondition(cursor, sort)
                );

        // 정렬 로직
        if ("popular".equals(sort)) {
            query.orderBy(popularityScore.desc(), club.id.desc());
        } else if ("member_count".equals(sort)) {
            // 멤버 수 서브쿼리를 정렬에 사용하기 위해 numberTemplate으로 감싸 NumberExpression으로 변환
            query.orderBy(Expressions.numberTemplate(Long.class, "({0})", memberCountSubQuery).desc(), club.id.desc());
        } else { // "latest"
            query.orderBy(club.createdAt.desc(), club.id.desc());
        }

        query.limit(size + 1);

        return query.fetch();
    }

    // 솔직히 개똥코드 인거 알지만....일단 돌아감...
    private List<ClubCardQueryResponse> getClubListWithNativeQuery(
            ClubCategory category,
            ClubType type,
            Boolean recruiting,
            List<ApplicableGrade> grades,
            int size,
            String cursor,
            String sort,
            String userEmail) {

        // 동적 SQL 생성
        StringBuilder sql = new StringBuilder();
        sql.append("""
        SELECT c.id, c.name, c.club_type, c.club_category, c.custom_category, 
               c.summary, c.profile_img, 
               (SELECT COUNT(*) FROM club_members cm WHERE cm.club_id = c.id) as member_count,
               c.recruiting,
               (SELECT COUNT(*) > 0 FROM user_favorites uf WHERE uf.club_id = c.id AND uf.user_id = (SELECT id FROM users WHERE email = ?)) as bookmarked
        FROM clubs c
        WHERE 1=1
        """);

        int paramIndex = 1;

        // 카테고리 조건 추가
        if (category != null) {
            sql.append(" AND c.club_category = ?");
        }

        // 타입 조건 추가
        if (type != null) {
            sql.append(" AND c.club_type = ?");
        }

        // 모집여부 조건 추가
        if (recruiting != null) {
            sql.append(" AND c.recruiting = ?");
        }

        // 학년 조건 추가
        if (grades != null && !grades.isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < grades.size(); i++) {
                if (i > 0) sql.append(" OR ");
                sql.append("c.target_grades::text LIKE ?");
            }
            sql.append(")");
        }

        switch (sort) {
            case "latest":
                sql.append(" ORDER BY c.id DESC");
                break;
            case "member_count":
                sql.append(" ORDER BY (SELECT COUNT(*) FROM club_members cm WHERE cm.club_id = c.id) DESC");
                break;
            case "popular":
                sql.append(" ORDER BY ((SELECT COUNT(*) FROM club_members cm WHERE cm.club_id = c.id) * 0.7 + COALESCE((SELECT COUNT(*) FROM user_favorites uf2 WHERE uf2.club_id = c.id), 0) * 0.3) DESC");
                break;
            default:
                sql.append(" ORDER BY c.created_at DESC");
        }
        sql.append(" LIMIT ?");

        Query query = entityManager.createNativeQuery(sql.toString());

        // 파라미터 설정
        paramIndex = 1;

        // userEmail (항상 첫 번째)
        query.setParameter(paramIndex++, userEmail);

        // 조건별 파라미터 설정
        if (category != null) {
            query.setParameter(paramIndex++, category.name());
        }

        if (type != null) {
            query.setParameter(paramIndex++, type.name());
        }

        if (recruiting != null) {
            query.setParameter(paramIndex++, recruiting);
        }

        if (grades != null && !grades.isEmpty()) {
            for (ApplicableGrade grade : grades) {
                query.setParameter(paramIndex++, "%\"" + grade.name() + "\"%");
            }
        }

        // size (항상 마지막)
        query.setParameter(paramIndex, size + 1);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new ClubCardQueryResponse(
                        (String) row[0],  // id
                        (String) row[1],  // name
                        ClubType.valueOf((String) row[2]),  // clubType
                        ClubCategory.valueOf((String) row[3]),  // clubCategory
                        (String) row[4],  // customCategory
                        (String) row[5],  // summary
                        (String) row[6],  // profileImageUrl
                        ((Number) row[7]).intValue(),  // memberCount
                        (Boolean) row[8],  // recruiting
                        (Boolean) row[9]   // bookmarked
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClubCardQueryResponse> searchByKeyword(String keyword, int size, String cursor, String sort, String userEmail) {
        // Subquery for member count
        JPQLQuery<Integer> memberCountSubQuery = JPAExpressions
                .select(clubMember.count().intValue())
                .from(clubMember)
                .where(clubMember.club.id.eq(club.id));

        // Subquery for bookmarked status
        JPQLQuery<Boolean> bookmarkedSubQuery = JPAExpressions
                .select(favorite.count().gt(0))
                .from(favorite)
                .where(favorite.club.id.eq(club.id).and(favorite.user.email.eq(userEmail)));

        // Main query
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
                        // Apply cursor condition from existing method
                        cursorCondition(cursor, sort)
                );

        // Sorting logic
        String currentSort = (sort == null) ? "latest" : sort;
        switch (currentSort) {
            case "popular":
                JPQLQuery<Long> favoriteCountSubQuery = JPAExpressions
                        .select(favorite.count())
                        .from(favorite)
                        .where(favorite.club.id.eq(club.id));

                NumberExpression<Double> popularityScore = Expressions.numberTemplate(Double.class,
                        "({0}) * 0.7 + ({1}) * 0.3",
                        Expressions.numberTemplate(Long.class, "({0})", memberCountSubQuery), // Corrected line
                        favoriteCountSubQuery);
                query.orderBy(popularityScore.desc(), club.id.desc());
                break;
            case "member":
                // To order by a subquery, it needs to be a NumberExpression
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

}
