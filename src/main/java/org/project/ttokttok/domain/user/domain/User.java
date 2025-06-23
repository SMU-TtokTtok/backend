package org.project.ttokttok.domain.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.project.ttokttok.global.entity.BaseTimeEntity;

@Entity
@Getter
@Setter
public class User extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;        // 202220255@sangmyung.kr

    @Column(nullable = false)
    private String password;     // BCrypt 암호화

    @Column(nullable = false)
    private String name;         // 실명

    private boolean isEmailVerified = false; // 이메일 인증 여부
}
