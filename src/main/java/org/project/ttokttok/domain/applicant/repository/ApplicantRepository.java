package org.project.ttokttok.domain.applicant.repository;

import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantRepository extends JpaRepository<Applicant, String>, ApplicantCustomRepository {
}