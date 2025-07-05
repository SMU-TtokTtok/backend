package org.project.ttokttok.domain.club.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.springframework.stereotype.Repository;

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
                        applyForm.applyDeadline,
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
        return favorite.user.email.eq(email)
                .and(favorite.club.id.eq(clubId))
                .isNotNull();
    }

    private NumberExpression<Integer> getClubMemberCount(String clubId) {
        return clubMember.club.id.eq(clubId)
                .count()
                .intValue();
    }
}
