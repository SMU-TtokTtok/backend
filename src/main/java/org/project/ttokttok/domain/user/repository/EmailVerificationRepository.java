package org.project.ttokttok.domain.user.repository;

import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 이메일로 최신 인증코드 조회 (만료되지 않은 것 중에서)
    Optional<EmailVerification> findFirstByEmailAndIsVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, LocalDateTime now);

    // 이메일과 코드로 인증 정보 조회
    Optional<EmailVerification> findByEmailAndCodeAndIsVerifiedFalse(String email, String code);

    // 특정 이메일의 모든 미인증 코드들을 만료된 것으로 처리
    @Modifying
    @Query("UPDATE EmailVerification e SET e.isVerified = true WHERE e.email = :email AND e.isVerified = false")
    void expireAllPendingVerifications(@Param("email") String email);

    // 만료된 인증코드들 정리 (배치 작업용)
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    void deleteExpiredVerifications(@Param("now") LocalDateTime now);

    // 특정 이메일의 인증 완료 여부 확인
    boolean existsByEmailAndIsVerifiedTrue(String email);
}
