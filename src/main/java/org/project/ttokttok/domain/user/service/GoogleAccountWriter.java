package org.project.ttokttok.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.domain.enums.AuthProvider;
import org.project.ttokttok.domain.user.exception.GoogleLinkTargetNotFoundException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.global.auth.jwt.service.OnboardingTokenProvider.OnboardingClaims;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 구글 계정 생성/연동 DB 쓰기 전용 컴포넌트
 *
 * 오케스트레이터({@link GoogleOAuthService})와 분리된 별도 빈으로 둠으로써
 * 각 메서드가 독립된 트랜잭션 경계를 갖는다. 이 경계 덕분에:
 * 1) 유니크 제약 위반 시 {@code saveAndFlush}가 이 트랜잭션 안에서 예외를 확정 발생시키고,
 * 2) 해당 트랜잭션이 롤백된 뒤 예외가 호출부로 전파되어,
 * 3) 오케스트레이터가 트랜잭션 밖에서 catch 후 새 트랜잭션으로 승자(winner)를 재조회할 수 있다.
 *
 * (self-invocation 은 프록시를 우회해 새 트랜잭션이 열리지 않으므로 반드시 별도 빈이어야 한다.)
 */
@Component
@RequiredArgsConstructor
public class GoogleAccountWriter {

    private final UserRepository userRepository;

    /**
     * 온보딩 완료 시점에 구글 계정을 생성한다. (트랜잭션 경계 = 이 메서드)
     * 토큰 발급과 완료 사이의 상태 변화를 재확인한다: 이미 가입됐으면 그 계정, 같은 이메일의
     * 로컬 계정이 생겼으면 자동 연동, 아니면 신규 가입.
     *
     * @throws DataIntegrityViolationException 동시 가입 경쟁 시 (호출부에서 멱등 복구)
     */
    @Transactional
    public User createGoogleUser(OnboardingClaims claims, String name) {
        Optional<User> bySub = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, claims.sub());
        if (bySub.isPresent()) {
            return bySub.get();
        }

        Optional<User> byEmail = userRepository.findByEmail(claims.email());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.linkGoogle(claims.sub());
            return userRepository.saveAndFlush(existing);
        }

        return userRepository.saveAndFlush(User.signUpWithGoogle(claims.email(), name, claims.sub()));
    }

    /**
     * 기존 이메일 계정에 구글 계정을 연동한다. (트랜잭션 경계 = 이 메서드)
     * 관리 상태 엔티티를 다루기 위해 트랜잭션 내부에서 재조회 후 연동한다.
     *
     * @throws DataIntegrityViolationException 병렬 연동 경쟁 시 (호출부에서 멱등 복구)
     * @throws GoogleLinkTargetNotFoundException 재조회 시점에 대상 계정이 사라진 경우
     */
    @Transactional
    public User autoLink(String email, String sub) {
        // 트랜잭션 밖 조회와 이 재조회 사이에 계정이 삭제된 경쟁 상황. 이메일은 메시지에 담지 않는다.
        User user = userRepository.findByEmail(email)
                .orElseThrow(GoogleLinkTargetNotFoundException::new);
        user.linkGoogle(sub); // 다른 sub 와 이미 연동 시 GoogleAccountConflictException
        return userRepository.saveAndFlush(user);
    }
}
