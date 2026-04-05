package org.project.ttokttok.domain.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Nested
    @DisplayName("회원가입(signUp) 테스트")
    class SignUpTest {

        @Test
        @DisplayName("올바른 정보로 회원가입 객체를 생성할 수 있다")
        void signUp_Success() {
            // given
            String email = "test@sangmyung.kr";
            String password = "encodedPassword123";
            String name = "홍길동";
            boolean termsAgreed = true;

            // when
            User user = User.signUp(email, password, name, termsAgreed);

            // then
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPassword()).isEqualTo(password);
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.isTermsAgreed()).isTrue();
            assertThat(user.getId()).isNull(); // JPA 저장 전이므로 null
        }

        @Test
        @DisplayName("약관 동의를 하지 않으면 회원가입이 불가능하다")
        void signUp_Fail_TermsNotAgreed() {
            // given
            String email = "test@sangmyung.kr";
            String password = "encodedPassword123";
            String name = "홍길동";

            // when & then
            assertThatThrownBy(() -> User.signUp(email, password, name, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("약관 동의가 필요합니다.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("이메일이 비어있으면 회원가입이 불가능하다")
        void signUp_Fail_EmailEmpty(String email) {
            assertThatThrownBy(() -> User.signUp(email, "password", "name", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일은 필수입니다.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid-email", "test.com", "test@", "test@gmail.com"})
        @DisplayName("이메일 형식이 올바르지 않으면 회원가입이 불가능하다")
        void signUp_Fail_EmailInvalid(String email) {
            assertThatThrownBy(() -> User.signUp(email, "password", "name", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("올바른 이메일 형식이 아닙니다.");
        }

        @Test
        @DisplayName("이름이 2자 미만이면 회원가입이 불가능하다")
        void signUp_Fail_NameTooShort() {
            assertThatThrownBy(() -> User.signUp("test@sangmyung.kr", "pass", "김", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이름은 최소 2자 이상이어야 합니다.");
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("비밀번호를 업데이트할 수 있다")
        void updatePassword_Success() {
            // given
            User user = User.signUp("test@sangmyung.kr", "oldPass", "홍길동", true);
            String newPass = "newEncodedPass";

            // when
            user.updatePassword(newPass);

            // then
            assertThat(user.getPassword()).isEqualTo(newPass);
        }

        @Test
        @DisplayName("이메일 인증 상태를 변경할 수 있다")
        void verifyEmail_Success() {
            // given
            User user = User.signUp("test@sangmyung.kr", "pass", "홍길동", true);
            
            // then
            assertThat(user.isEmailVerified()).isTrue();
            
            // 이미 인증된 상태에서 다시 호출 시 예외 발생 검증
            assertThatThrownBy(user::verifyEmail)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("이미 인증된 이메일입니다.");
        }
    }
}
