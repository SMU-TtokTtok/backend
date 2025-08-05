package org.project.ttokttok.domain.clubMember.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberCountQueryResponse;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

import static org.project.ttokttok.domain.clubMember.domain.MemberRole.*;
import static org.project.ttokttok.domain.clubMember.domain.QClubMember.clubMember;

@Repository
@RequiredArgsConstructor
public class ClubMemberCustomRepositoryImpl implements ClubMemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public ClubMemberPageQueryResponse findClubMemberPageByClubId(String clubId,
                                                                  int pageNum,
                                                                  int pageSize) {

        // TODO: NO OFFSET으로 개선하기.
        List<ClubMember> members = queryFactory
                .selectFrom(clubMember)
                .where(clubMember.club.id.eq(clubId))
                .orderBy(
                        orderByRoleAsc(),
                        clubMember.grade.asc()
                )
                .offset((long) (pageNum - 1) * pageSize)
                .limit(pageSize)
                .fetch();

        // 결과 추출
        long totalCount = getTotalCount(clubId);

        // 총 페이지 수 계산
        int totalPage = (int) Math.ceil((double) totalCount / pageSize);

        return ClubMemberPageQueryResponse.builder()
                .currentPage(pageNum)
                .totalPage(totalPage)
                .totalCount((int) totalCount)
                .clubMembers(members)
                .build();
    }

    @Override
    public ClubMemberCountQueryResponse countClubMembersByClubId(String clubId) {
        Tuple data = getClubMemberCounts(clubId);

        return ClubMemberCountQueryResponse.builder()
                .totalCount((int) getTotalCount(clubId))
                .firstGradeCount(data.get(0, Long.class) != null ? data.get(0, Long.class).intValue() : 0)
                .secondGradeCount(data.get(1, Long.class) != null ? data.get(1, Long.class).intValue() : 0)
                .thirdGradeCount(data.get(2, Long.class) != null ? data.get(2, Long.class).intValue() : 0)
                .fourthGradeCount(data.get(3, Long.class) != null ? data.get(3, Long.class).intValue() : 0)
                .build();
    }

    private long getTotalCount(String clubId) {
        return queryFactory
                .select(clubMember.count())
                .from(clubMember)
                .where(clubMember.club.id.eq(clubId))
                .fetchOne();
    }

    // 총 회원 수와 학년별 회원 수를 조회하는 메서드
    private Tuple getClubMemberCounts(String clubId) {
        return queryFactory
                .select(
                        clubMember.grade.when(Grade.FIRST_GRADE).then(1L).otherwise(0L).sum(),
                        clubMember.grade.when(Grade.SECOND_GRADE).then(1L).otherwise(0L).sum(),
                        clubMember.grade.when(Grade.THIRD_GRADE).then(1L).otherwise(0L).sum(),
                        clubMember.grade.when(Grade.FOURTH_GRADE).then(1L).otherwise(0L).sum()
                )
                .from(clubMember)
                .where(clubMember.club.id.eq(clubId))
                .fetchOne();
    }

    private BooleanExpression executiveOrMember(String role) {
        if (!role.equalsIgnoreCase(EXECUTIVE.name()))
            return clubMember.role.eq(MEMBER);
        else
            return clubMember.role.in(PRESIDENT, VICE_PRESIDENT, EXECUTIVE);
    }

    public OrderSpecifier<Integer> orderByRoleAsc() {
        return new CaseBuilder()
                .when(clubMember.role.eq(MemberRole.PRESIDENT)).then(0)
                .when(clubMember.role.eq(MemberRole.VICE_PRESIDENT)).then(1)
                .when(clubMember.role.eq(MemberRole.EXECUTIVE)).then(2)
                .when(clubMember.role.eq(MemberRole.MEMBER)).then(3)
                .otherwise(4) // 나머지 예외처리도 고려
                .asc();
    }
}
