package org.project.ttokttok.domain.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.project.ttokttok.global.entity.BaseTimeEntity;

@Entity
@Getter
@Setter
public class User extends BaseTimeEntity {
    // PK
    @Id
    private String id;  // UUID → String으로 변경

    // 이메일
    @Column(unique = true, nullable = false)
    private String email;

    // 비밀번호
    @Column(nullable = false)
    private String password;

    // 이름
    @Column(nullable = false)
    private String name;

    // 이메일 검증
    private boolean isEmailVerified = false;

    // 약관 동의
    private boolean termsAgreed = false;  // 새로 추가
}
