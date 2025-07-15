package org.project.ttokttok.domain.user.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class EmailVerification extends BaseTimeEntity {
    // PK
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이메일
    @Column(nullable = false)
    private String email;

    // 인증코드
    @Column(nullable = false, length = 6)
    private String code;  // 6자리 인증코드

    // 인증 됐는지 안됐는지?
    private boolean isVerified = false;

    // 만료시간
    @Column(nullable = false)
    private LocalDateTime expiresAt;  // 만료 시간 (5분)

    // 이메일 인증
    @Builder
    public EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    // 인증 완료 처리
    public void markAsVerified() {
        this.isVerified = true;
    }

    // 만료 여부 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    // 코드 일치 여부 확인
    public boolean isCodeMatch(String inputCode) {
        return this.code.equals(inputCode);
    }
}
