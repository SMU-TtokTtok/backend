package org.project.ttokttok.domain.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.admin.exception.AdminPasswordNotMatchException;
import org.project.ttokttok.global.entity.BaseTimeEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Entity
@Table(name = "admins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin extends BaseTimeEntity {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Getter
    @Column(nullable = false, updatable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Builder
    private Admin(String username, String password) {
        validateUsernameFormat(username);
        validatePasswordFormat(password);
        this.username = username;
        this.password = password;
    }

    // ------- 정적 메서드 -------
    public static Admin adminJoin(String username, String password) {
        return Admin.builder()
                .username(username)
                .password(password)
                .build();
    }

    // ------- 검증용 메서드 -------
    public void validatePassword(String password, PasswordEncoder pe) {
        if (!pe.matches(password, this.password))
            throw new AdminPasswordNotMatchException();
    }

    // ------- 비즈니스 로직 메서드 -------
    public void resetPassword(String newPassword, PasswordEncoder pe) {
        this.password = pe.encode(newPassword);
    }

    // ------- 내부 검증 메서드 -------
    private void validateUsernameFormat(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("관리자 명은 null이거나 비어있을 수 없습니다.");
        }
        if (username.length() < 8 || username.length() > 20) {
            throw new IllegalArgumentException("관리자 명은 8자 이상 20자 이하여야 합니다.");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("관리자 명은 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.");
        }
    }

    private void validatePasswordFormat(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 null이거나 비어 있을 수 없습니다.");
        }
    }
}
