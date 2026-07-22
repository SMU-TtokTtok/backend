package org.project.ttokttok.global.auth.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.global.auth.oauth.dto.GoogleUserInfo;
import org.project.ttokttok.global.auth.oauth.exception.InvalidGoogleTokenException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * 구글 ID 토큰 검증기
 *
 * JwtDecoder(서명/iss/aud/exp 검증)를 감싸 검증 실패를 도메인 예외로 변환하고
 * 사용자 정보를 추출한다. 보안상 ID 토큰 원문은 절대 로깅하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleIdTokenVerifier {

    private final JwtDecoder googleJwtDecoder;

    /**
     * 구글 ID 토큰을 검증하고 사용자 정보를 추출한다.
     *
     * @param idToken 프론트엔드가 전달한 구글 ID 토큰
     * @return 검증된 구글 사용자 정보 (sub, email, emailVerified, name)
     * @throws InvalidGoogleTokenException 서명 위조, 만료, iss/aud 불일치 등 모든 검증 실패
     */
    public GoogleUserInfo verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidGoogleTokenException();
        }

        try {
            Jwt jwt = googleJwtDecoder.decode(idToken);
            GoogleUserInfo userInfo = GoogleUserInfo.from(jwt);
            log.debug("구글 ID 토큰 검증 성공: sub={}, email={}", userInfo.sub(), userInfo.email());
            return userInfo;
        } catch (JwtException e) {
            // aud 불일치도 동일 메시지로 처리하여 공격자에게 실패 원인을 노출하지 않는다
            log.warn("구글 ID 토큰 검증 실패: {}", e.getMessage());
            throw new InvalidGoogleTokenException();
        }
    }
}
