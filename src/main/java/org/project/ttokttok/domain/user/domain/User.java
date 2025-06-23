package org.project.ttokttok.domain.user.domain;

<<<<<<< HEAD
import java.util.UUID;

import jakarta.validation.constraints.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.project.ttokttok.global.entity.BaseTimeEntity;
import org.project.ttokttok.global.validation.annotation.StrongPassword;
=======
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.project.ttokttok.global.entity.BaseTimeEntity;
>>>>>>> e1209fb ([#4] - User 도메인 기본 구조 생성)

@Entity
@Getter
@Setter
public class User extends BaseTimeEntity {
<<<<<<< HEAD
    // 1. ID 타입을 UUID로 변경
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 2. 이메일 검증 어노테이션
    @Column(unique = true, nullable = false)
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Pattern(regexp = ".*@sangmyung\\.kr$", message = "상명대 이메일만 사용 가능합니다")
    private String email;        // 202021000@sangmyung.kr

    // 3. 비밀번호 검증 추가
    @Column(nullable = false)
    @StrongPassword
=======
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;        // 202220255@sangmyung.kr

    @Column(nullable = false)
>>>>>>> e1209fb ([#4] - User 도메인 기본 구조 생성)
    private String password;     // BCrypt 암호화

    @Column(nullable = false)
    private String name;         // 실명

    private boolean isEmailVerified = false; // 이메일 인증 여부
}
