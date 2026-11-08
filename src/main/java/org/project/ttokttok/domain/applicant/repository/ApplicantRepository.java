package org.project.ttokttok.domain.applicant.repository;

import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicantRepository extends JpaRepository<Applicant, String>, ApplicantCustomRepository {
    List<Applicant> findByApplyFormId(String applyFormId);

    // FETCH JOIN을 사용하여 DocumentPhase와 Memos를 함께 조회
    @Query("SELECT a FROM Applicant a " +
            "LEFT JOIN FETCH a.documentPhase dp " +
            "LEFT JOIN FETCH dp.memos " +
            "WHERE a.id = :applicantId")
    Optional<Applicant> findByIdWithDocumentPhase(@Param("applicantId") String applicantId);

    // 제출된 모든 서류 답변. 첨부 파일 참조 집합을 만드는 정리 스케줄러가 쓴다.
    @Query("SELECT dp.answers FROM DocumentPhase dp")
    List<List<Answer>> findAllDocumentAnswers();

    boolean existsByUserEmailAndApplyFormId(String email, String formId);
}