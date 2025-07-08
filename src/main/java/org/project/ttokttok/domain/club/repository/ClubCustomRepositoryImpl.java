package org.project.ttokttok.domain.club.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.springframework.stereotype.Repository;

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

            // 페이징 관련
            int size,               // 조회할 개수
            String cursor,          // 커서 (무한스크롤용)
            String sort,            // 정렬 방식 (현재 미사용)

            // 사용자 관련
            String userEmail) {     // 즐겨찾기 확인용 사용자 이메일

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
                        cursorCondition(cursor)     // 커서 조건 (무한스크롤)
                )
                .orderBy(club.id.desc())            // ID 내림차순 정렬 (최신순)
                .limit(size + 1);                   // size + 1개 조회 (hashNext 판단용)

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

    private BooleanExpression cursorCondition(String cursor) {
        return cursor != null ? club.id.lt(cursor) : null;
    }
}
