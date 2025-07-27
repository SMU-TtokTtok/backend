package org.project.ttokttok.domain.applicant.repository;

import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApplicantRepository extends JpaRepository<Applicant, String>, ApplicantCustomRepository {
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Applicant a WHERE a.applyForm.id = :applyFormId and a.status != 'EVALUATING'")
    int deleteAllApplicantsByApplyFormId(String applyFormId);

    List<Applicant> findByApplyFormId(String applyFormId);

    /**
     * 중복 지원 체크를 위한 메서드
     */
    boolean existsByUserEmailAndApplyFormId(String userEmail, String applyFormId);
}