package org.project.ttokttok.domain.applyform.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.repository.dto.ClubRecruitmentQueryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplyFormRepository extends JpaRepository<ApplyForm, String> {

    Optional<ApplyForm> findTopByClubIdAndStatusOrderByCreatedAtDesc(String clubId, ApplyFormStatus status);

    List<ApplyForm> findByClubId(String clubId);

    Optional<ApplyForm> findByClubIdAndStatus(String clubId, ApplyFormStatus status);

    Optional<ApplyForm> findTopByClubIdOrderByCreatedAtDesc(String clubId);

    boolean existsByClubIdAndStatus(String clubId, ApplyFormStatus applyFormStatus);

    @Query("SELECT t.tempData FROM ApplyForm a "
            + "INNER JOIN TempApplicant t "
            + "ON t.formId = a.id "
            + "WHERE a.club.id = :clubId AND t.userEmail = :userEmail")
    Map<String, Object> findTempData(String userEmail, String clubId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Applicant a WHERE a.applyForm.id = :formId")
    int deleteAllApplicantByFormId(String formId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TempApplicant t WHERE t.formId = :formId")
    int deleteAllTempApplicantByFormId(String formId);

    @Query("SELECT a FROM ApplyForm a WHERE a.status = 'ACTIVE' AND a.applyEndDate < :currentDate")
    List<ApplyForm> findExpiredApplyForms(@Param("currentDate") LocalDate currentDate);

    /**
     * 여러 동아리의 모집중 지원폼을 한 번에 조회
     *
     * <p>모집 종료는 {@code isRecruiting} 만 false 로 바뀌고 {@code status} 는 ACTIVE 로 남는다
     * ({@code ApplyFormScheduler}, {@code ClubAdminService#toggleRecruitment}). 그래서 모집 여부를
     * 판정하려면 두 조건을 함께 봐야 한다.
     */
    @Query("SELECT new org.project.ttokttok.domain.applyform.repository.dto.ClubRecruitmentQueryDto("
            + "a.club.id, a.applyEndDate) "
            + "FROM ApplyForm a "
            + "WHERE a.club.id IN :clubIds AND a.status = 'ACTIVE' AND a.isRecruiting = true")
    List<ClubRecruitmentQueryDto> findRecruitingFormsByClubIds(@Param("clubIds") List<String> clubIds);
}
