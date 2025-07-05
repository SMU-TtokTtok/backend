package org.project.ttokttok.domain.applyform.repository;

import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplyFormRepository extends JpaRepository<ApplyForm, String>{
    Optional<ApplyForm> findByClubId(String clubId);
}
