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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Getter
    @Column(nullable = false, updatable = false, unique = true)
    private String username;

    @Lob
    @Column(nullable = false)
    private String password;

    // 추후 One to one 동아리 연관관계 맺기

    @Builder
    private Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // ------- 정적 메서드 -------
    // todo: 추후 api에 맞게 수정할 것
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

}
