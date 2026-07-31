package org.project.ttokttok.domain.applicant.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.dto.ApplicantSimpleInfoDto;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.repository.dto.UserApplicationHistoryQueryResponse;
import org.project.ttokttok.domain.applicant.repository.dto.response.ApplicantPageQueryResponse;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.project.ttokttok.domain.applicant.domain.QApplicant.applicant;
import static org.project.ttokttok.domain.applyform.domain.QApplyForm.applyForm;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.club.domain.QClub.club;
import static org.project.ttokttok.domain.clubMember.domain.QClubMember.clubMember;
import static org.project.ttokttok.domain.favorite.domain.QFavorite.favorite;

@Repository
@RequiredArgsConstructor
public class ApplicantCustomRepositoryImpl implements ApplicantCustomRepository {

    private final JPAQueryFactory queryFactory;

    private static final String SUBMIT = "SUBMIT";

    @Override
    public ApplicantPageQueryResponse findApplicantsPageWithSortCriteria(String sortCriteria,
                                                                         boolean evaluating,
                                                                         int cursor,
                                                                         int size,
                                                                         String applyFormId,
                                                                         String kind) {
        return getApplicantPageQueryResponse(
                sortCriteria,
                evaluating,
                cursor,
                size,
                applyFormId,
                null,
                kind);
    }

    @Override
    public ApplicantPageQueryResponse searchApplicantsByKeyword(String searchKeyword,
                                                                String sortCriteria,
                                                                boolean evaluating,
                                                                int cursor,
                                                                int size,
                                                                String applyFormId,
                                                                String kind) {
        // 검색 키워드와 조건에 맞는 지원자 조회
        return getApplicantPageQueryResponse(
                sortCriteria,
                evaluating,
                cursor,
                size,
                applyFormId,
                searchKeyword,
                kind);
    }

    @Override
    public ApplicantPageQueryResponse findApplicantsByStatus(boolean isPassed,
                                                             int cursor,
                                                             int size,
                                                             String applyFormId,
                                                             String kind) {

        PhaseStatus status = isPassed ? PhaseStatus.PASS : PhaseStatus.FAIL;

        return getApplicantPageQueryResponse(
                null,
                false,
                cursor,
                size,
                applyFormId,
                null,
                kind,
                status
        );
    }

    // 오버로딩된 메서드, status가 없는 경우
    private ApplicantPageQueryResponse getApplicantPageQueryResponse(String sortCriteria,
                                                                     boolean evaluating,
                                                                     int cursor,
                                                                     int size,
                                                                     String applyFormId,
                                                                     String searchKeyword,
                                                                     String kind) {

        return getApplicantPageQueryResponse(sortCriteria, evaluating, cursor, size,
                applyFormId, searchKeyword, kind, null);
    }

    // 지원자 페이지 조회를 위한 공통 메서드
    private ApplicantPageQueryResponse getApplicantPageQueryResponse(String sortCriteria,
                                                                     boolean evaluating,
                                                                     int cursor,
                                                                     int size,
                                                                     String applyFormId,
                                                                     String searchKeyword,
                                                                     String kind,
                                                                     PhaseStatus status) {

        ApplicantPhaseQuery phaseQuery = ApplicantPhaseQuery.from(kind);

        // 총 개수 조회
        Long count = getApplicantCount(phaseQuery, applyFormId, searchKeyword, evaluating, status);

        // 지원자 목록 조회
        List<ApplicantSimpleInfoDto> applicants = phaseQuery.createBaseQuery(queryFactory)
                .where(phaseFilters(phaseQuery, applyFormId, searchKeyword, evaluating, status))
                .orderBy(
                        getSortCriteria(sortCriteria),
                        applicant.id.asc() // 기본적으로 ID로 정렬하여 일관성 유지
                )
                .limit(size)
                .offset((long) size * (cursor - 1))
                .fetch();

        int totalPage = (int) Math.ceil((double) count / size);

        return ApplicantPageQueryResponse.builder()
                .currentPage(cursor)
                .totalPage(totalPage)
                .totalCount(count.intValue())
                .applicants(applicants)
                .build();
    }

    // baseQuery 에서 지원자 수를 조회하는 메서드
    private Long getApplicantCount(ApplicantPhaseQuery phaseQuery,
                                   String applyFormId,
                                   String searchKeyword,
                                   boolean evaluating,
                                   PhaseStatus statusFilter) {

        JPAQuery<Long> query = queryFactory
                .select(applicant.count())
                .from(applicant);

        return phaseQuery.joinPhase(query)
                .where(phaseFilters(phaseQuery, applyFormId, searchKeyword, evaluating, statusFilter))
                .fetchOne();
    }

    /**
     * 목록 조회와 개수 조회가 공유하는 필터 조건.
     * 두 쿼리가 항상 같은 조건을 보도록 한곳에서 만든다.
     */
    private BooleanExpression[] phaseFilters(ApplicantPhaseQuery phaseQuery,
                                             String applyFormId,
                                             String searchKeyword,
                                             boolean evaluating,
                                             PhaseStatus status) {
        return new BooleanExpression[]{
                applicant.applyForm.id.eq(applyFormId),
                phaseQuery.phaseLink(),
                containsName(searchKeyword),
                phaseQuery.evaluatingOnly(evaluating),
                phaseQuery.statusEq(status)
        };
    }

    //FIXME: 학년순으로 제대로 정렬 안되는 이슈 해결 필요
    // 들어온 sortCriteria에 따라 정렬 조건을 반환하는 메서드
    private OrderSpecifier<?> getSortCriteria(@Nullable String sortCriteria) {
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            return applicant.grade.asc();
        }

        switch (sortCriteria.toUpperCase()) {
            case SUBMIT:
                return applicant.createdAt.asc();
            default:
                return applicant.grade.asc();
        }
    }

    // ---- BOOLEAN EXPRESSION METHODS ---- //
    private BooleanExpression containsName(String searchKeyword) {
        return searchKeyword != null ? applicant.name.contains(searchKeyword) : null;
    }

    @Override
    public List<UserApplicationHistoryQueryResponse> getUserApplicationHistory(String userEmail,
                                                                              int size,
                                                                              String cursor,
                                                                              String sort) {
        JPAQuery<UserApplicationHistoryQueryResponse> query = queryFactory
                .select(Projections.constructor(
                        UserApplicationHistoryQueryResponse.class,
                        applicant.id,
                        club.id,
                        club.name,
                        club.clubType,
                        club.clubCategory,
                        club.customCategory,
                        club.summary,
                        club.profileImageUrl,
                        getClubMemberCount(),
                        hasActiveApplyForm(),
                        // 즐겨찾기 여부 확인 (서브쿼리로 처리)
                        isFavorite(userEmail),
                        applicant.currentPhase,
                        applicant.createdAt,
                        applyForm.applyEndDate  // 마감 임박 계산을 위한 지원 마감일 추가
                ))
                .from(applicant)
                .innerJoin(applicant.applyForm, applyForm)
                .innerJoin(applyForm.club, club)
                .where(
                        applicant.userEmail.eq(userEmail),
                        cursorCondition(cursor, sort)
                )
                .orderBy(getApplicationSortCriteria(sort))
                .limit(size + 1); // hasNext 확인을 위해 +1

        return query.fetch();
    }

    /**
     * 사용자 지원내역 정렬 조건 생성
     *
     * <p>현재 {@code sort} 값은 정렬 결과에 영향을 주지 않는다.
     * {@code popular} / {@code member_count} 정렬이 아직 구현되지 않아 {@code latest} 와
     * 동일하게 최신순으로 동작한다. {@code sort} 는 공개 API 시그니처이므로 유지하되,
     * 실제 분기는 해당 정렬이 구현될 때 추가한다.
     */
    private OrderSpecifier<?>[] getApplicationSortCriteria(String sort) {
        return new OrderSpecifier[]{
                applicant.createdAt.desc(),
                applicant.id.desc() // 일관성을 위한 보조 정렬
        };
    }

    /**
     * 커서 기반 페이징을 위한 조건 생성
     *
     * <p>정렬 기준과 무관하게 ID 기반 단일 커서를 사용한다.
     * 복합 커서는 위 정렬이 실제로 구현될 때 함께 도입한다.
     */
    private BooleanExpression cursorCondition(String cursor, String sort) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }

        return applicant.id.lt(cursor);
    }

    /**
     * 동아리 멤버수 조회 (서브쿼리)
     */
    private JPQLQuery<Integer> getClubMemberCount() {
        return JPAExpressions.select(clubMember.count().coalesce(0L).intValue())
                .from(clubMember)
                .where(clubMember.club.eq(club));
    }

    /**
     * 즐겨찾기 여부 확인 (서브쿼리)
     */
    private BooleanExpression isFavorite(String userEmail) {
        if (userEmail == null) {
            return Expressions.asBoolean(false);
        }
        return JPAExpressions.selectOne()
                .from(favorite)
                .where(favorite.user.email.eq(userEmail)
                        .and(favorite.club.eq(club)))
                .exists();
    }

    /**
     * 활성 지원폼 존재 여부 확인 (모집중 여부)
     */
    private BooleanExpression hasActiveApplyForm() {
        return JPAExpressions.selectOne()
                .from(applyForm)
                .where(applyForm.club.eq(club)
                        .and(applyForm.status.eq(ACTIVE)))
                .exists();
    }

}
