package org.project.ttokttok.domain.applicant.repository;

import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicantRepository extends JpaRepository<Applicant, String>, ApplicantCustomRepository {
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Applicant a WHERE a.applyForm.id = :applyFormId and a.status != 'EVALUATING'")
    int deleteAllApplicantsByApplyFormId(String applyFormId);

    List<Applicant> findByApplyFormId(String applyFormId);

    // FETCH JOIN을 사용하여 DocumentPhase와 Memos를 함께 조회
    @Query("SELECT a FROM Applicant a " +
            "LEFT JOIN FETCH a.documentPhase dp " +
            "LEFT JOIN FETCH dp.memos " +
            "WHERE a.id = :applicantId")
    Optional<Applicant> findByIdWithDocumentPhase(@Param("applicantId") String applicantId);
}