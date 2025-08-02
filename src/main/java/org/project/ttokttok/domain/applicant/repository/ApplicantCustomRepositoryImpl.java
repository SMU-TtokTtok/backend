package org.project.ttokttok.domain.applicant.repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.dto.ApplicantSimpleInfoDto;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.repository.dto.response.ApplicantPageQueryResponse;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static org.project.ttokttok.domain.applicant.domain.QApplicant.applicant;
import static org.project.ttokttok.domain.applicant.domain.QDocumentPhase.documentPhase;
import static org.project.ttokttok.domain.applicant.domain.QInterviewPhase.interviewPhase;
import static org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase.*;
import static org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus.*;

@Repository
@RequiredArgsConstructor
public class ApplicantCustomRepositoryImpl implements ApplicantCustomRepository {

    private final JPAQueryFactory queryFactory;

    private static final String INTERVIEW = "INTERVIEW";
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
                        containsName(searchKeyword),
                        isEvaluating(evaluating),
                        hasStatus(status)
                )
                .orderBy(getSortCriteria(sortCriteria))
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
        boolean isInterview = INTERVIEW.equalsIgnoreCase(kind);

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
                        containsName(searchKeyword),
                        isEvaluating(evaluating),
                        hasStatus(statusFilter)
                ).fetchOne();
    }

    // 들어온 sortCriteria에 따라 정렬 조건을 반환하는 메서드
    private OrderSpecifier<?> getSortCriteria(@Nullable String sortCriteria) {
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            return applicant.grade.asc(); // 기본 정렬은 학년 오름차순
        }

        switch (sortCriteria.toUpperCase()) {
            case SUBMIT:
                return applicant.createdAt.asc();
            default:
                return applicant.grade.asc();
        }
    }

    private JPAQuery<ApplicantSimpleInfoDto> createBaseQuery(String kind) {
        boolean isInterview = INTERVIEW.equalsIgnoreCase(kind);

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
    // 검색 키워드가 있는 경우 이름에 포함되는지 조건 추가
    private BooleanExpression containsName(String searchKeyword) {
        return searchKeyword != null ? applicant.name.contains(searchKeyword) : null;
    }

    // 평가 중 여부 확인
    private BooleanExpression isEvaluating(boolean evaluating) {
        return evaluating ? applicant.currentPhase.in(DOCUMENT_EVALUATING, INTERVIEW_EVALUATING) : null;
    }

    // 현재 단계에 따라 상태 결정 로직
    private BooleanExpression hasStatus(PhaseStatus status) {
        if (status == null) return null;

        // 상태별로 직접 조건 생성
        switch (status) {
            case EVALUATING:
                return applicant.currentPhase.in(DOCUMENT_EVALUATING, INTERVIEW_EVALUATING);
            case PASS:
                return applicant.currentPhase.in(DOCUMENT_PASS, INTERVIEW_PASS);
            case FAIL:
                return applicant.currentPhase.in(DOCUMENT_FAIL, INTERVIEW_FAIL);
            default:
                return null;
        }
    }

    //----EXPRESSION METHODS----//
    private Expression<?> getExpression(String kind) {
        return kind.equalsIgnoreCase(INTERVIEW) ?
                interviewPhase.interviewDate :
                Expressions.nullExpression(LocalDate.class);
    }

    // 서류 or 면접 상태 파악
    private Expression<?> getPhaseStatus(boolean isInterview) {
        return isInterview ? getInterviewStatus() : getDocumentStatus();
    }

    // 서류 상태
    private StringExpression/*<PhaseStatus>*/ getDocumentStatus() {
        return new CaseBuilder()
                .when(applicant.currentPhase.eq(DOCUMENT_EVALUATING))
                .then("EVALUATING")
                .when(applicant.currentPhase.eq(DOCUMENT_PASS))
                .then("PASS")
                .otherwise("FAIL");
    }

    // 면접 상태
    private StringExpression/*<PhaseStatus>*/ getInterviewStatus() {
        return new CaseBuilder()
                .when(applicant.currentPhase.in(INTERVIEW_EVALUATING))
                .then("EVALUATING")
                .when(applicant.currentPhase.eq(INTERVIEW_PASS))
                .then("PASS")
                .otherwise("FAIL");
    }

}
