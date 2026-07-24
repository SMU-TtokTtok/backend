package org.project.ttokttok.domain.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.user.domain.enums.AuthProvider;
import org.project.ttokttok.domain.user.exception.GoogleAccountConflictException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * User 엔티티의 구글 OAuth 관련 도메인 로직 테스트
 */
class UserOAuthTest {

    private static final String SUB = "google-sub-123";

    @Nested
    @DisplayName("signUpWithGoogle 정적 팩토리")
    class SignUpWithGoogleTest {

        @Test
        @DisplayName("상명대 도메인이 아닌 이메일(gmail)로도 가입할 수 있다")
        void signUpWithGoogle_withGmail_succeeds() {
            // when
            User user = User.signUpWithGoogle("user@gmail.com", "홍길동", SUB);

            // then
            assertThat(user.getEmail()).isEqualTo("user@gmail.com");
            assertThat(user.getPassword()).isNull(); // OAuth 전용 계정
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.isTermsAgreed()).isTrue();
            assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(user.getProviderId()).isEqualTo(SUB);
            assertThat(user.isOAuthOnly()).isTrue();
        }

        @Test
        @DisplayName("구글 sub가 없으면 예외를 던진다")
        void signUpWithGoogle_withoutSub_throws() {
            assertThatThrownBy(() -> User.signUpWithGoogle("user@gmail.com", "홍길동", null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> User.signUpWithGoogle("user@gmail.com", "홍길동", " "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("이메일이 없으면 예외를 던진다")
        void signUpWithGoogle_withoutEmail_throws() {
            assertThatThrownBy(() -> User.signUpWithGoogle(null, "홍길동", SUB))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("기존 signUp 정적 팩토리 (회귀)")
    class SignUpRegressionTest {

        @Test
        @DisplayName("여전히 상명대 이메일이 아니면 가입할 수 없다 (도메인 제한 유지)")
        void signUp_withNonSangmyungEmail_stillRejected() {
            assertThatThrownBy(() -> User.signUp("user@gmail.com", "encoded", "홍길동", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("올바른 이메일 형식이 아닙니다.");
        }

        @Test
        @DisplayName("여전히 비밀번호 없이 가입할 수 없다")
        void signUp_withoutPassword_stillRejected() {
            assertThatThrownBy(() -> User.signUp("a@sangmyung.kr", null, "홍길동", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호는 필수입니다.");
        }

        @Test
        @DisplayName("정상 가입 시 LOCAL 프로바이더로 생성되며 OAuth 전용이 아니다")
        void signUp_createsLocalUser() {
            // when
            User user = User.signUp("a@sangmyung.kr", "encoded", "홍길동", true);

            // then
            assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
            assertThat(user.getProviderId()).isNull();
            assertThat(user.isOAuthOnly()).isFalse();
        }
    }

    @Nested
    @DisplayName("linkGoogle 메서드")
    class LinkGoogleTest {

        @Test
        @DisplayName("로컬 계정에 구글 계정을 연동하면 provider 정보가 설정되고 비밀번호는 유지된다")
        void linkGoogle_onLocalAccount_linksAndKeepsPassword() {
            // given
            User user = User.signUp("a@sangmyung.kr", "encoded", "홍길동", true);

            // when
            user.linkGoogle(SUB);

            // then
            assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(user.getProviderId()).isEqualTo(SUB);
            assertThat(user.getPassword()).isEqualTo("encoded"); // 비밀번호 로그인 유지
            assertThat(user.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("같은 sub로 다시 연동하면 아무 일도 일어나지 않는다 (멱등)")
        void linkGoogle_sameSubTwice_isIdempotent() {
            // given
            User user = User.signUp("a@sangmyung.kr", "encoded", "홍길동", true);
            user.linkGoogle(SUB);

            // when & then (예외 없음)
            user.linkGoogle(SUB);
            assertThat(user.getProviderId()).isEqualTo(SUB);
        }

        @Test
        @DisplayName("다른 sub와 이미 연동되어 있으면 GoogleAccountConflictException을 던진다")
        void linkGoogle_differentSub_throwsConflict() {
            // given
            User user = User.signUp("a@sangmyung.kr", "encoded", "홍길동", true);
            user.linkGoogle(SUB);

            // when & then
            assertThatThrownBy(() -> user.linkGoogle("different-sub"))
                    .isInstanceOf(GoogleAccountConflictException.class);
        }
    }
}
