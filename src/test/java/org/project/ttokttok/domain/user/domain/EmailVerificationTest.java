package org.project.ttokttok.domain.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class EmailVerificationTest {

    // Test Constants
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_CODE = "123456";

    // ===== markAsVerified 테스트 =====

    @Nested
    @DisplayName("markAsVerified 메서드")
    class MarkAsVerifiedTest {

        @Test
        @DisplayName("인증 완료 처리 시 isVerified가 true로 변경된다")
        void markAsVerifiedChangesStatusToTrue() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(VALID_EMAIL)
                    .code(VALID_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            // when
            emailVerification.markAsVerified();

            // then
            assertThat(emailVerification.isVerified()).isTrue();
        }

        @Test
        @DisplayName("초기 상태에서 isVerified는 false이다")
        void initialVerificationStatusIsFalse() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(VALID_EMAIL)
                    .code(VALID_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            // then
            assertThat(emailVerification.isVerified()).isFalse();
        }
    }

    // ===== isExpired 테스트 =====

    @Nested
    @DisplayName("isExpired 메서드")
    class IsExpiredTest {

        @Test
        @DisplayName("만료 시간이 지나지 않으면 false를 반환한다")
        void isNotExpiredWhenTimeNotPassed() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(VALID_EMAIL)
                    .code(VALID_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            // when & then
            assertThat(emailVerification.isExpired()).isFalse();
        }

        @Test
        @DisplayName("만료 시간이 지나면 true를 반환한다")
        void isExpiredWhenTimePassed() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(VALID_EMAIL)
                    .code(VALID_CODE)
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();

            // when & then
            assertThat(emailVerification.isExpired()).isTrue();
        }
    }

    // ===== isCodeMatch 테스트 =====

    @Nested
    @DisplayName("isCodeMatch 메서드")
    class IsCodeMatchTest {

        @Test
        @DisplayName("올바른 코드를 입력하면 true를 반환한다")
        void matchesWhenCorrectCodeProvided() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(VALID_EMAIL)
                    .code(VALID_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            // when & then
            assertThat(emailVerification.isCodeMatch(VALID_CODE)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"654321", "000000", "111111", "123457"})
        @DisplayName("잘못된 코드를 입력하면 false를 반환한다")
        void doesNotMatchWhenIncorrectCodeProvided(String wrongCode) {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(VALID_EMAIL)
                    .code(VALID_CODE)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            // when & then
            assertThat(emailVerification.isCodeMatch(wrongCode)).isFalse();
        }
    }
}

