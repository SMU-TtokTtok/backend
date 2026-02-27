package org.project.ttokttok.domain.user.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationRepositoryTest implements RepositoryTestSupport {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_CODE = "123456";

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Nested
    @DisplayName("findFirstByEmailAndIsVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc 메서드")
    class FindLatestPendingVerificationTest {

        @Test
        @DisplayName("유효한 인증코드를 최신 순으로 조회할 수 있다")
        void findLatestPendingVerification_ReturnsLatest() {
            // given
            EmailVerification oldVerification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code("111111")
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            emailVerificationRepository.save(oldVerification);

            EmailVerification latestVerification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            emailVerificationRepository.save(latestVerification);

            // when
            Optional<EmailVerification> found = emailVerificationRepository
                    .findFirstByEmailAndIsVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                            TEST_EMAIL, LocalDateTime.now());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getCode()).isEqualTo(TEST_CODE);
        }

        @Test
        @DisplayName("만료된 인증코드는 조회되지 않는다")
        void findLatestPendingVerification_ExpiredNotReturned() {
            // given
            EmailVerification expiredVerification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            emailVerificationRepository.save(expiredVerification);

            // when
            Optional<EmailVerification> found = emailVerificationRepository
                    .findFirstByEmailAndIsVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                            TEST_EMAIL, LocalDateTime.now());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("이미 인증된 코드는 조회되지 않는다")
        void findLatestPendingVerification_VerifiedNotReturned() {
            // given
            EmailVerification verifiedCode = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            verifiedCode.markAsVerified();
            emailVerificationRepository.save(verifiedCode);

            // when
            Optional<EmailVerification> found = emailVerificationRepository
                    .findFirstByEmailAndIsVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                            TEST_EMAIL, LocalDateTime.now());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmailAndCodeAndIsVerifiedFalse 메서드")
    class FindByEmailAndCodeTest {

        @Test
        @DisplayName("이메일과 코드로 미인증 상태의 인증정보를 조회할 수 있다")
        void findByEmailAndCode_ReturnsVerification() {
            // given
            EmailVerification verification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            emailVerificationRepository.save(verification);

            // when
            Optional<EmailVerification> found = emailVerificationRepository
                    .findByEmailAndCodeAndIsVerifiedFalse(TEST_EMAIL, TEST_CODE);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(found.get().getCode()).isEqualTo(TEST_CODE);
        }

        @Test
        @DisplayName("이미 인증된 코드는 조회되지 않는다")
        void findByEmailAndCode_VerifiedNotReturned() {
            // given
            EmailVerification verification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            verification.markAsVerified();
            emailVerificationRepository.save(verification);

            // when
            Optional<EmailVerification> found = emailVerificationRepository
                    .findByEmailAndCodeAndIsVerifiedFalse(TEST_EMAIL, TEST_CODE);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("expireAllPendingVerifications 메서드")
    class ExpireAllPendingVerificationsTest {

        @Test
        @DisplayName("특정 이메일의 모든 미인증 코드들을 인증 처리한다")
        void expireAllPendingVerifications_MarksAllAsVerified() {
            // given
            EmailVerification verification1 = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code("111111")
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            EmailVerification verification2 = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code("222222")
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            emailVerificationRepository.save(verification1);
            emailVerificationRepository.save(verification2);

            // when
            emailVerificationRepository.expireAllPendingVerifications(TEST_EMAIL);

            // then
            Optional<EmailVerification> found = emailVerificationRepository
                    .findFirstByEmailAndIsVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                            TEST_EMAIL, LocalDateTime.now());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteExpiredVerifications 메서드")
    class DeleteExpiredVerificationsTest {

        @Test
        @DisplayName("만료된 인증코드들을 삭제한다")
        void deleteExpiredVerifications_DeletesExpired() {
            // given
            EmailVerification expiredVerification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().minusMinutes(10))
                    .build();
            EmailVerification validVerification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code("654321")
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            emailVerificationRepository.save(expiredVerification);
            emailVerificationRepository.save(validVerification);

            // when
            emailVerificationRepository.deleteExpiredVerifications(LocalDateTime.now());

            // then
            long count = emailVerificationRepository.count();
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("existsByEmailAndIsVerifiedTrue 메서드")
    class ExistsByEmailAndIsVerifiedTrueTest {

        @Test
        @DisplayName("인증 완료된 이메일이 존재하면 true를 반환한다")
        void existsByEmailAndIsVerifiedTrue_ReturnsTrue() {
            // given
            EmailVerification verification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            verification.markAsVerified();
            emailVerificationRepository.save(verification);

            // when
            boolean exists = emailVerificationRepository.existsByEmailAndIsVerifiedTrue(TEST_EMAIL);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("인증 완료된 이메일이 없으면 false를 반환한다")
        void existsByEmailAndIsVerifiedTrue_ReturnsFalse() {
            // given
            EmailVerification verification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            emailVerificationRepository.save(verification);

            // when
            boolean exists = emailVerificationRepository.existsByEmailAndIsVerifiedTrue(TEST_EMAIL);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByEmailAndCodeAndIsVerifiedTrue 메서드")
    class ExistsByEmailAndCodeAndIsVerifiedTrueTest {

        @Test
        @DisplayName("이메일과 코드가 일치하고 인증 완료된 경우 true를 반환한다")
        void existsByEmailAndCodeAndIsVerifiedTrue_ReturnsTrue() {
            // given
            EmailVerification verification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            verification.markAsVerified();
            emailVerificationRepository.save(verification);

            // when
            boolean exists = emailVerificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(TEST_EMAIL, TEST_CODE);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("코드가 일치하지 않으면 false를 반환한다")
        void existsByEmailAndCodeAndIsVerifiedTrue_WrongCode_ReturnsFalse() {
            // given
            EmailVerification verification = EmailVerification.builder()
                    .email(TEST_EMAIL)
                    .code(TEST_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            verification.markAsVerified();
            emailVerificationRepository.save(verification);

            // when
            boolean exists = emailVerificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(TEST_EMAIL, "000000");

            // then
            assertThat(exists).isFalse();
        }
    }
}


