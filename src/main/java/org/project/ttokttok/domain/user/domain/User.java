package org.project.ttokttok.domain.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.global.entity.BaseTimeEntity;

/**
 * 사용자 엔티티 (Rich Domain Model)
 * 객체의 불변성을 유지하고 의미 있는 비즈니스 메서드를 통해 상태를 관리합니다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Column(unique = true, nullable = false, updatable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isEmailVerified;

    @Column(nullable = false)
    private boolean termsAgreed;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String email, String password, String name, boolean isEmailVerified, boolean termsAgreed) {
        validateEmail(email);
        validatePassword(password);
        validateName(name);

        this.email = email;
        this.password = password;
        this.name = name;
        this.isEmailVerified = isEmailVerified;
        this.termsAgreed = termsAgreed;
    }

    /**
     * 회원가입을 위한 정적 팩토리 메서드
     * 
     * @param email 가입 이메일
     * @param encodedPassword 암호화된 비밀번호
     * @param name 사용자 이름
     * @param termsAgreed 약관 동의 여부
     * @return 생성된 User 객체
     */
    public static User signUp(String email, String encodedPassword, String name, boolean termsAgreed) {
        if (!termsAgreed) {
            throw new IllegalArgumentException("약관 동의가 필요합니다.");
        }
        
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .isEmailVerified(true) // 가입 시점에 인증 완료 상태여야 함
                .termsAgreed(true)
                .build();
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

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
    
        // 나중에 수정 - 상명대 이메일 정규식
        if (!email.toLowerCase().endsWith("@sangmyung.kr")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    private void validatePassword(String password) {
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
