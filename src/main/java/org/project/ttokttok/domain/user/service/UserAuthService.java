package org.project.ttokttok.domain.user.service;

import org.springframework.stereotype.Service;

@Service
public class UserAuthService {

    // 이메일 인증코드 전송
    public void sendVerificationCode(String email) {
        // 1. 상명대 이메일 형식 검증
        // 2. 6자리 랜덤 코드 생성
        // 3. Redis 에 저장 (5분 TTL)
        // 4. 이메일 발송
    }

    // 회원가입
//    public UserResponse signup(SignupRequest request) {
//        // 1. 인증코드 검증
//        // 2. 비밀번호 확인 일치 검증
//        // 3. 이메일 중복 검증
//        // 4. 회원 정보 저장
//        // 5. JWT 토큰 발급
//    }

    // 로그인
//    public LoginResponse login(LoginRequest request) {
//        // 1. 이메일로 사용자 조회
//        // 2. 비밀번호 검증
//        // 3. 이메일 인증 확인
//        // 4. JWT 토큰 발급
//    }
}
