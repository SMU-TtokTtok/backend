package org.project.ttokttok.domain.superadmin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.superadmin.exception.SuperAdminPasswordNotMatchException;
import org.project.ttokttok.global.entity.BaseTimeEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Table(name = "super_admins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SuperAdmin extends BaseTimeEntity {

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
    private SuperAdmin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // ------- 정적 메서드 -------
    public static SuperAdmin create(String username, String password) {
        return SuperAdmin.builder()
                .username(username)
                .password(password)
                .build();
    }

    // ------- 검증용 메서드 -------
    public void validatePassword(String rawPassword, PasswordEncoder passwordEncoder) {
        if (!passwordEncoder.matches(rawPassword, this.password)) {
            throw new SuperAdminPasswordNotMatchException();
        }
    }
}
