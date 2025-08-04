package org.project.ttokttok.domain.applicant.repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
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

import java.time.LocalDate;
import java.util.List;

import static org.project.ttokttok.domain.applicant.domain.QApplicant.applicant;
import static org.project.ttokttok.domain.applicant.domain.QDocumentPhase.documentPhase;
import static org.project.ttokttok.domain.applicant.domain.QInterviewPhase.interviewPhase;
import static org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus.*;
import static org.project.ttokttok.domain.applyform.domain.QApplyForm.applyForm;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.club.domain.QClub.club;
import static org.project.ttokttok.domain.clubMember.domain.QClubMember.clubMember;
import static org.project.ttokttok.domain.favorite.domain.QFavorite.favorite;

@Repository
@RequiredArgsConstructor
public class ApplicantCustomRepositoryImpl implements ApplicantCustomRepository {

    private final JPAQueryFactory queryFactory;

    private static final String INTERVIEW_STRING = "INTERVIEW";
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

        PhaseStatus status = isPassed ? PASS : FAIL;

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
        // 기본 쿼리 생성
        JPAQuery<ApplicantSimpleInfoDto> baseQuery = createBaseQuery(kind);

        // 총 개수 조회 (수정된 버전)
        Long count = getApplicantCount(applyFormId, searchKeyword, evaluating, kind, status);

        // 지원자 목록 조회
        List<ApplicantSimpleInfoDto> applicants = baseQuery
                .where(
                        applicant.applyForm.id.eq(applyFormId),
                        kind.equalsIgnoreCase(INTERVIEW_STRING) ?
                                interviewPhase.applicant.eq(applicant) :
                                documentPhase.applicant.eq(applicant),
                        containsName(searchKeyword),
                        kind.equalsIgnoreCase(INTERVIEW_STRING) ?
                                interviewEvaluatingCheck(evaluating) :
                                documentEvaluatingCheck(evaluating),
                        kind.equalsIgnoreCase(INTERVIEW_STRING) ?
                                interviewStatusCheck(status) :
                                documentStatusCheck(status)
                )
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
    private Long getApplicantCount(String applyFormId,
                                   String searchKeyword,
                                   boolean evaluating,
                                   String kind,
                                   PhaseStatus statusFilter) {
        boolean isInterview = INTERVIEW_STRING.equalsIgnoreCase(kind);

        JPAQuery<Long> query = queryFactory
                .select(applicant.count())
                .from(applicant);

        if (isInterview) {
            query.leftJoin(interviewPhase).on(interviewPhase.applicant.eq(applicant));
        } else {
            query.leftJoin(documentPhase).on(documentPhase.applicant.eq(applicant));
        }

        return query
                .where(
                        applicant.applyForm.id.eq(applyFormId),
                        kind.equalsIgnoreCase(INTERVIEW_STRING) ?
                                interviewPhase.applicant.eq(applicant) :
                                documentPhase.applicant.eq(applicant),
                        containsName(searchKeyword),
                        kind.equalsIgnoreCase(INTERVIEW_STRING) ?
                                interviewEvaluatingCheck(evaluating) :
                                documentEvaluatingCheck(evaluating),
                        kind.equalsIgnoreCase(INTERVIEW_STRING) ?
                                interviewStatusCheck(statusFilter) :
                                documentStatusCheck(statusFilter)
                ).fetchOne();
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

    private JPAQuery<ApplicantSimpleInfoDto> createBaseQuery(String kind) {
        boolean isInterview = INTERVIEW_STRING.equalsIgnoreCase(kind);

        // IF문으로 안전하게 분기처리
        if (isInterview) {
            return queryFactory
                    .select(Projections.constructor(
                            ApplicantSimpleInfoDto.class,
                            applicant.id,
                            applicant.grade,
                            applicant.name,
                            applicant.major,
                            getPhaseStatus(true),
                            interviewPhase.interviewDate
                    ))
                    .from(applicant)
                    .leftJoin(interviewPhase)
                    .on(interviewPhase.applicant.eq(applicant));
        } else {
            return queryFactory
                    .select(Projections.constructor(
                            ApplicantSimpleInfoDto.class,
                            applicant.id,
                            applicant.grade,
                            applicant.name,
                            applicant.major,
                            getPhaseStatus(false),
                            Expressions.nullExpression(LocalDate.class)
                    ))
                    .from(applicant)
                    .leftJoin(documentPhase)
                    .on(documentPhase.applicant.eq(applicant));
        }
    }

    // ---- BOOLEAN EXPRESSION METHODS ---- //
    private BooleanExpression containsName(String searchKeyword) {
        return searchKeyword != null ? applicant.name.contains(searchKeyword) : null;
    }

    private BooleanExpression documentEvaluatingCheck(boolean evaluating) {
        return evaluating ? applicant.documentPhase.status.eq(EVALUATING) : null;
    }

    private BooleanExpression interviewEvaluatingCheck(boolean evaluating) {
        return evaluating ? applicant.interviewPhase.status.eq(EVALUATING) : null;
    }

    private BooleanExpression documentStatusCheck(PhaseStatus status) {
        if (status == null) return null;

        return switch (status) {
            case EVALUATING -> applicant.documentPhase.status.eq(EVALUATING);
            case PASS -> applicant.documentPhase.status.eq(PASS);
            case FAIL -> applicant.documentPhase.status.eq(FAIL);
            default -> null;
        };
    }

    private BooleanExpression interviewStatusCheck(PhaseStatus status) {
        if (status == null) return null;

        return switch (status) {
            case EVALUATING -> applicant.interviewPhase.status.eq(EVALUATING);
            case PASS -> applicant.interviewPhase.status.eq(PASS);
            case FAIL -> applicant.interviewPhase.status.eq(FAIL);
            default -> null;
        };
    }

    //----EXPRESSION METHODS----//

    // 서류 or 면접 상태 파악
    private Expression<?> getPhaseStatus(boolean isInterview) {
        return isInterview ? getInterviewStatus() : getDocumentStatus();
    }

    // 서류 상태
    private StringExpression/*<PhaseStatus>*/ getDocumentStatus() {
        return new CaseBuilder()
                .when(applicant.documentPhase.status.eq(EVALUATING))
                .then("EVALUATING")
                .when(applicant.documentPhase.status.eq(PASS))
                .then("PASS")
                .otherwise("FAIL");
    }

    // 면접 상태
    private StringExpression/*<PhaseStatus>*/ getInterviewStatus() {
        return new CaseBuilder()
                .when(applicant.interviewPhase.status.eq(EVALUATING))
                .then("EVALUATING")
                .when(applicant.interviewPhase.status.eq(PASS))
                .then("PASS")
                .otherwise("FAIL");
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
                        applicant.createdAt
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
     */
    private OrderSpecifier<?>[] getApplicationSortCriteria(String sort) {
        return switch (sort.toLowerCase()) {
            case "latest" -> new OrderSpecifier[]{
                    applicant.createdAt.desc(),
                    applicant.id.desc() // 일관성을 위한 보조 정렬
            };
            case "popular", "member_count" -> new OrderSpecifier[]{
                    // 멤버수 기준 정렬 (서브쿼리 사용)
                    applicant.createdAt.desc(), // 임시로 날짜순 정렬 (성능상 이유)
                    applicant.id.desc()
            };
            default -> new OrderSpecifier[]{
                    applicant.createdAt.desc(),
                    applicant.id.desc()
            };
        };
    }

    /**
     * 커서 기반 페이징을 위한 조건 생성
     */
    private BooleanExpression cursorCondition(String cursor, String sort) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }

        // 간단한 구현: ID 기반 커서
        // 실제로는 정렬 기준에 따라 복합 커서를 구현해야 함
        return switch (sort.toLowerCase()) {
            case "latest" -> applicant.id.lt(cursor);
            case "popular", "member_count" -> applicant.id.lt(cursor);
            default -> applicant.id.lt(cursor);
        };
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
