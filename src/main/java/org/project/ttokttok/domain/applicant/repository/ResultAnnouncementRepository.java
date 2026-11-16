package org.project.ttokttok.domain.applicant.repository;

import java.util.Optional;
import org.project.ttokttok.domain.applicant.domain.ResultAnnouncement;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultAnnouncementRepository extends JpaRepository<ResultAnnouncement, String> {

    Optional<ResultAnnouncement> findByClubIdAndIdempotencyKey(String clubId, String idempotencyKey);

    boolean existsByApplyFormIdAndPhase(String applyFormId, ApplicantPhase phase);
}
