package org.project.ttokttok.domain.applyform.repository;

import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplyFormRepository extends JpaRepository<ApplyForm, String> {
    Optional<ApplyForm> findTopByClubIdOrderByCreatedAtDesc(String clubId);

    List<ApplyForm> findByClubId(String clubId);
}
