package org.project.ttokttok.domain.applyform.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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
}
