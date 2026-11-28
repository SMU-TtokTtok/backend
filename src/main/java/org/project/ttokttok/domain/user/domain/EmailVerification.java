package org.project.ttokttok.domain.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {
    // 인증된 코드로 회원가입·비밀번호 재설정을 마쳐야 하는 시간
    private static final long VERIFIED_VALIDITY_MINUTES = 30;

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
    private EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    // 인증 완료 처리. 인증된 코드는 회원가입·재설정에 한 번 쓰이거나 만료되면 효력을 잃는다.
    public void markAsVerified() {
        this.isVerified = true;
        this.expiresAt = LocalDateTime.now().plusMinutes(VERIFIED_VALIDITY_MINUTES);
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
