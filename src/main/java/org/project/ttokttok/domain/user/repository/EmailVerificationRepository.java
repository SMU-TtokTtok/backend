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
    Optional<EmailVerification> findFirstByEmailAndIsVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
            String email, LocalDateTime now);

    // 이메일과 코드로 인증 정보 조회
    Optional<EmailVerification> findByEmailAndCodeAndIsVerifiedFalse(String email, String code);

    // 특정 이메일의 아직 유효한 미인증 코드들을 만료시킨다 (인증 상태는 바꾸지 않는다)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE EmailVerification e SET e.expiresAt = :now "
            + "WHERE e.email = :email AND e.isVerified = false AND e.expiresAt > :now")
    void expireAllPendingVerifications(@Param("email") String email, @Param("now") LocalDateTime now);

    // 인증됐고 만료되지 않은 코드를 만료시켜 한 번만 쓰이게 한다. 갱신된 행 수(0 또는 1)를 돌려준다.
    // 한 문장으로 확인과 소비를 함께 해서 같은 코드로 동시에 들어온 요청 중 하나만 성공한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE EmailVerification e SET e.expiresAt = :now "
            + "WHERE e.email = :email AND e.code = :code AND e.isVerified = true AND e.expiresAt > :now")
    int consumeVerifiedCode(@Param("email") String email, @Param("code") String code, @Param("now") LocalDateTime now);

    // 만료된 인증코드들 정리 (배치 작업용)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    void deleteExpiredVerifications(@Param("now") LocalDateTime now);
}
