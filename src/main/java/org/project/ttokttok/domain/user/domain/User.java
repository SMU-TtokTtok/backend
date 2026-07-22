package org.project.ttokttok.domain.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.user.domain.enums.AuthProvider;
import org.project.ttokttok.domain.user.exception.GoogleAccountConflictException;
import org.project.ttokttok.global.entity.BaseTimeEntity;

/**
 * 사용자 엔티티 (Rich Domain Model)
 * 객체의 불변성을 유지하고 의미 있는 비즈니스 메서드를 통해 상태를 관리합니다.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        // 동일 구글 계정(sub)이 두 사용자 행에 연결되는 것을 방지 (Flyway V23와 동일, H2 테스트 스키마용)
        @UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = {"provider", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Column(unique = true, nullable = false, updatable = false)
    private String email;

    // OAuth 전용 계정은 비밀번호가 없으므로 NULL 허용 (V23 마이그레이션)
    @Column
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isEmailVerified;

    @Column(nullable = false)
    private boolean termsAgreed;

    // 인증 제공자 (LOCAL: 이메일+비밀번호, GOOGLE: 구글 OAuth)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider = AuthProvider.LOCAL;

    // 구글 sub (불변 식별자). 이메일은 구글 측에서 변경될 수 있으므로 sub 로 식별한다.
    @Column(name = "provider_id")
    private String providerId;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String email, String password, String name, boolean isEmailVerified,
                 boolean termsAgreed, AuthProvider provider, String providerId) {
        validateEmailFormat(email);
        validateName(name);

        this.email = email;
        this.password = password;
        this.name = name;
        this.isEmailVerified = isEmailVerified;
        this.termsAgreed = termsAgreed;
        this.provider = provider;
        this.providerId = providerId;
    }

    /**
     * 회원가입을 위한 정적 팩토리 메서드 (이메일+비밀번호 가입)
     *
     * @param email 가입 이메일 (상명대 이메일만 허용)
     * @param encodedPassword 암호화된 비밀번호
     * @param name 사용자 이름
     * @param termsAgreed 약관 동의 여부
     * @return 생성된 User 객체
     */
    public static User signUp(String email, String encodedPassword, String name, boolean termsAgreed) {
        if (!termsAgreed) {
            throw new IllegalArgumentException("약관 동의가 필요합니다.");
        }
        validateSangmyungEmail(email);
        validatePassword(encodedPassword);

        return User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .isEmailVerified(true) // 가입 시점에 인증 완료 상태여야 함
                .termsAgreed(true)
                .provider(AuthProvider.LOCAL)
                .build();
    }

    /**
     * 구글 OAuth 가입을 위한 정적 팩토리 메서드
     * 구글이 이메일을 검증하므로 도메인 제한 없이 허용하고, 비밀번호는 존재하지 않는다.
     *
     * @param email 구글 계정 이메일 (email_verified=true 검증 완료 전제)
     * @param name 사용자 이름 (온보딩 화면에서 입력받은 값)
     * @param providerId 구글 sub (불변 식별자)
     * @return 생성된 User 객체
     */
    public static User signUpWithGoogle(String email, String name, String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("구글 식별자(sub)는 필수입니다.");
        }

        return User.builder()
                .email(email)
                .password(null) // OAuth 전용 계정
                .name(name)
                .isEmailVerified(true) // 구글이 이메일 검증을 보증
                .termsAgreed(true) // 온보딩 완료 API 에서 동의 검증 후 호출
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .build();
    }

    /**
     * 기존 계정에 구글 계정을 연동한다. (같은 sub 재연동은 멱등 처리)
     *
     * @param sub 구글 sub (불변 식별자)
     * @throws GoogleAccountConflictException 이미 다른 구글 계정과 연동된 경우
     */
    public void linkGoogle(String sub) {
        if (this.providerId != null) {
            if (this.providerId.equals(sub)) {
                return; // 이미 같은 구글 계정과 연동됨 (멱등)
            }
            throw new GoogleAccountConflictException();
        }

        this.provider = AuthProvider.GOOGLE;
        this.providerId = sub;
        // 구글 검증 이메일 기반 연동이므로 미인증 상태라면 방어적으로 인증 처리
        this.isEmailVerified = true;
    }

    /**
     * OAuth 전용 계정 여부 (비밀번호 로그인 불가)
     */
    public boolean isOAuthOnly() {
        return this.password == null;
    }

    /**
     * 비밀번호 업데이트 비즈니스 메서드
     *
     * @param encodedPassword 암호화된 새 비밀번호
     */
    public void updatePassword(String encodedPassword) {
        validatePassword(encodedPassword);
        this.password = encodedPassword;
    }

    /**
     * 이메일 인증 상태 변경 메서드
     */
    public void verifyEmail() {
        if (this.isEmailVerified) {
            throw new IllegalStateException("이미 인증된 이메일입니다.");
        }
        this.isEmailVerified = true;
    }

    // --- 내부 검증 로직 (Rich Domain Model의 핵심) ---

    private static void validateEmailFormat(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
    }

    // 이메일+비밀번호 가입 경로에만 적용되는 상명대 도메인 제한
    private static void validateSangmyungEmail(String email) {
        validateEmailFormat(email);

        // 나중에 수정 - 상명대 이메일 정규식
        if (!email.toLowerCase().endsWith("@sangmyung.kr")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (name.length() < 2) {
            throw new IllegalArgumentException("이름은 최소 2자 이상이어야 합니다.");
        }
    }
}
