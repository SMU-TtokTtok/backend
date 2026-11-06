package org.project.ttokttok.domain.applicant.repository;

import org.project.ttokttok.domain.applicant.domain.enums.ApplicationHistorySort;
import org.project.ttokttok.domain.applicant.domain.enums.Sort;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.repository.dto.UserApplicationHistoryQueryResponse;
import org.project.ttokttok.domain.applicant.repository.dto.response.ApplicantPageQueryResponse;

import java.util.List;

public interface ApplicantCustomRepository {
    ApplicantPageQueryResponse findApplicantsPageWithSortCriteria(Sort sortCriteria,
                                                                  boolean evaluating,
                                                                  int cursor,
                                                                  int size,
                                                                  String applyFormId,
                                                                  ApplicantPhase kind);

    ApplicantPageQueryResponse searchApplicantsByKeyword(String searchKeyword,
                                                         Sort sortCriteria,
                                                         boolean evaluating,
                                                         int cursor,
                                                         int size,
                                                         String applyFormId,
                                                         ApplicantPhase kind);

    ApplicantPageQueryResponse findApplicantsByStatus(boolean isPassed,
                                                      int page,
                                                      int size,
                                                      String applyFormId,
                                                      ApplicantPhase kind);

    /**
     * 사용자의 동아리 지원내역 조회 (무한스크롤 지원)
     * 
     * @param userEmail 사용자 이메일
     * @param size 조회할 개수 (size+1로 조회하여 hasNext 확인)
     * @param cursor 커서 (무한스크롤용)
     * @param sort 정렬 방식 (latest, popular, member_count)
     * @return 사용자 지원내역 목록
     */
    List<UserApplicationHistoryQueryResponse> getUserApplicationHistory(String userEmail,
                                                                       int size,
                                                                       String cursor,
                                                                       ApplicationHistorySort sort);
}
