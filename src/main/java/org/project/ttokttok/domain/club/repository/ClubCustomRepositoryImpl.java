package org.project.ttokttok.domain.club.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.springframework.stereotype.Repository;

import static org.project.ttokttok.domain.applyform.domain.QApplyForm.applyForm;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.club.domain.QClub.club;
import static org.project.ttokttok.domain.favorite.domain.QFavorite.favorite;
import static org.project.ttokttok.domain.user.domain.QUser.user;

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
                        favorite.isNotNull(),
                        club.recruiting.isTrue(),
                        club.summary,
                        club.profileImageUrl,
                        user.count(),
                        applyForm.applyStartDate,
                        applyForm.applyDeadline,
                        applyForm.grades,
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
}
