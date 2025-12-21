package org.project.ttokttok.domain.temp.applicant.repository;

import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface TempApplicantRepository extends JpaRepository<TempApplicant, String> {
    Optional<TempApplicant> findByUserEmailAndFormId(String userEmail, String formId);

    List<TempApplicant> findAllByUserEmail(String userEmail);
}
