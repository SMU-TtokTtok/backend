package org.project.ttokttok.domain.admin.domain;

import jakarta.persistence.*;
import org.project.ttokttok.domain.admin.exception.AdminPasswordNotMatchException;
import org.project.ttokttok.global.entity.BaseTimeEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Entity
public class Admin extends BaseTimeEntity {

    // UUID 생성 전략
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Id
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Column(nullable = false, updatable = false, unique = true)
    private String username;

    @Lob
    @Column(nullable = false)
    private String password;

    // 추후 One to one 동아리 연관관계 맺기

    // ------- 검증용 메서드 -------

    public void validatePassword(String password, PasswordEncoder pe) {
        if (!pe.matches(password, this.password))
            throw new AdminPasswordNotMatchException();
    }
}
