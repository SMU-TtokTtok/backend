package org.project.ttokttok.domain.clubMember.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;
import org.springframework.stereotype.Repository;

import java.util.List;

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
                .orderBy(clubMember.grade.asc())
                .offset((long) (pageNum - 1) * pageSize)
                .limit(pageSize)
                .fetch();

        // 총 회원 수와 학년별 회원 수를 한 번의 쿼리로 조회
        Tuple counts = getClubMemberCounts(clubId);

        // 결과 추출
        long totalCount = counts.get(0, Long.class);
        long firstGradeCount = counts.get(1, Long.class);
        long secondGradeCount = counts.get(2, Long.class);
        long thirdGradeCount = counts.get(3, Long.class);
        long fourthGradeCount = counts.get(4, Long.class);

        // 총 페이지 수 계산
        int totalPage = (int) Math.ceil((double) totalCount / pageSize);

        return ClubMemberPageQueryResponse.builder()
                .currentPage(pageNum)
                .totalPage(totalPage)
                .totalCount((int) totalCount)
                .firstGradeCount((int) firstGradeCount)
                .secondGradeCount((int) secondGradeCount)
                .thirdGradeCount((int) thirdGradeCount)
                .fourthGradeCount((int) fourthGradeCount)
                .clubMembers(members)
                .build();
    }

    // 총 회원 수와 학년별 회원 수를 조회하는 메서드
    private Tuple getClubMemberCounts(String clubId) {
        return queryFactory
                .select(
                        clubMember.count(),
                        clubMember.grade.when(Grade.FIRST_GRADE).then(1L).otherwise(0L).sum(),
                        clubMember.grade.when(Grade.SECOND_GRADE).then(1L).otherwise(0L).sum(),
                        clubMember.grade.when(Grade.THIRD_GRADE).then(1L).otherwise(0L).sum(),
                        clubMember.grade.when(Grade.FOURTH_GRADE).then(1L).otherwise(0L).sum()
                )
                .from(clubMember)
                .where(clubMember.club.id.eq(clubId))
                .fetchOne();
    }
}
