package org.project.ttokttok.domain.applicant.repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.project.ttokttok.domain.applicant.domain.dto.ApplicantSimpleInfoDto;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;

import java.time.LocalDate;

import static org.project.ttokttok.domain.applicant.domain.QApplicant.applicant;
import static org.project.ttokttok.domain.applicant.domain.QDocumentPhase.documentPhase;
import static org.project.ttokttok.domain.applicant.domain.QInterviewPhase.interviewPhase;
import static org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus.EVALUATING;
import static org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus.PASS;

/**
 * 전형 단계(서류/면접)별 조회 전략
 *
 * <p>기존에는 {@code kind} 문자열을 매번 {@code equalsIgnoreCase} 로 비교하는 삼항 분기가
 * 조회 메서드 곳곳(where 절 3회, count 쿼리 3회, base 쿼리, 상태 표현식)에 흩어져 있었습니다.
 * 두 단계의 쿼리 구성이 대칭적이므로, 단계별 차이를 이 전략에 모아 호출부에서 분기를 없앴습니다.
 *
 * <p>새로운 전형 단계가 생기면 이 enum에 상수를 하나 추가하면 됩니다.
 */
public enum ApplicantPhaseQuery {

    /** 서류 전형 */
    DOCUMENT {
        @Override
        public JPAQuery<ApplicantSimpleInfoDto> createBaseQuery(JPAQueryFactory queryFactory) {
            return queryFactory
                    .select(Projections.constructor(
                            ApplicantSimpleInfoDto.class,
                            applicant.id,
                            applicant.grade,
                            applicant.name,
                            applicant.major,
                            statusExpression(),
                            // 서류 단계에는 면접 날짜가 없다
                            Expressions.nullExpression(LocalDate.class)
                    ))
                    .from(applicant)
                    .leftJoin(documentPhase)
                    .on(phaseLink());
        }

        @Override
        public <T> JPAQuery<T> joinPhase(JPAQuery<T> query) {
            return query.leftJoin(documentPhase).on(phaseLink());
        }

        @Override
        public BooleanExpression phaseLink() {
            return documentPhase.applicant.eq(applicant);
        }

        @Override
        public BooleanExpression statusEq(PhaseStatus status) {
            return status == null ? null : applicant.documentPhase.status.eq(status);
        }

        @Override
        protected Expression<?> statusExpression() {
            return new CaseBuilder()
                    .when(applicant.documentPhase.status.eq(EVALUATING)).then("EVALUATING")
                    .when(applicant.documentPhase.status.eq(PASS)).then("PASS")
                    .otherwise("FAIL");
        }
    },

    /** 면접 전형 */
    INTERVIEW {
        @Override
        public JPAQuery<ApplicantSimpleInfoDto> createBaseQuery(JPAQueryFactory queryFactory) {
            return queryFactory
                    .select(Projections.constructor(
                            ApplicantSimpleInfoDto.class,
                            applicant.id,
                            applicant.grade,
                            applicant.name,
                            applicant.major,
                            statusExpression(),
                            interviewPhase.interviewDate
                    ))
                    .from(applicant)
                    .leftJoin(interviewPhase)
                    .on(phaseLink());
        }

        @Override
        public <T> JPAQuery<T> joinPhase(JPAQuery<T> query) {
            return query.leftJoin(interviewPhase).on(phaseLink());
        }

        @Override
        public BooleanExpression phaseLink() {
            return interviewPhase.applicant.eq(applicant);
        }

        @Override
        public BooleanExpression statusEq(PhaseStatus status) {
            return status == null ? null : applicant.interviewPhase.status.eq(status);
        }

        @Override
        protected Expression<?> statusExpression() {
            return new CaseBuilder()
                    .when(applicant.interviewPhase.status.eq(EVALUATING)).then("EVALUATING")
                    .when(applicant.interviewPhase.status.eq(PASS)).then("PASS")
                    .otherwise("FAIL");
        }
    };

    private static final String INTERVIEW_KIND = "INTERVIEW";

    /**
     * 요청의 {@code kind} 문자열을 전략으로 변환한다.
     * {@code "INTERVIEW"}(대소문자 무관)만 면접 전형이고, 그 외 값은 모두 서류 전형으로 본다.
     */
    public static ApplicantPhaseQuery from(String kind) {
        return INTERVIEW_KIND.equalsIgnoreCase(kind) ? INTERVIEW : DOCUMENT;
    }

    /** 단계별 지원자 목록 조회의 기본 쿼리 (select + from + join) */
    public abstract JPAQuery<ApplicantSimpleInfoDto> createBaseQuery(JPAQueryFactory queryFactory);

    /** 이미 만들어진 쿼리에 해당 단계 전형 테이블을 조인한다. */
    public abstract <T> JPAQuery<T> joinPhase(JPAQuery<T> query);

    /** 지원자와 해당 단계 전형을 잇는 조인/필터 조건 */
    public abstract BooleanExpression phaseLink();

    /** 해당 단계의 상태 일치 조건 (status가 null이면 조건 없음) */
    public abstract BooleanExpression statusEq(PhaseStatus status);

    /** 평가중 필터 (evaluating이 false면 조건 없음) */
    public BooleanExpression evaluatingOnly(boolean evaluating) {
        return evaluating ? statusEq(EVALUATING) : null;
    }

    /** 조회 결과에 담을 전형 상태 문자열 표현식 */
    protected abstract Expression<?> statusExpression();
}
