package org.project.ttokttok.domain.temp.applyform.repository;

import org.project.ttokttok.domain.temp.applyform.domain.TempApplyForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface TempApplyFormRepository extends JpaRepository<TempApplyForm, String> {
    
    /**
     * 특정 동아리의 임시 지원폼을 조회
     */
    Optional<TempApplyForm> findByClubId(String clubId);
}
