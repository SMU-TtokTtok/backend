package org.project.ttokttok.global.auth.jwt.filter;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.admin.exception.AdminNotFoundException;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.global.auth.jwt.exception.InvalidRoleException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager {

    // jwt의 인증 권한을 반환해주는 클래스

    //private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    private final String ROLE_USER = "ROLE_USER";
    private final String ROLE_ADMIN = "ROLE_ADMIN";

    //todo: merge 후 주석 해제 및 상수 수정
    public Authentication getAuthentication(String email, String role) {
        Object principal = switch (role) {
            /*case "ROLE_USER" -> userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));*/
            case ROLE_ADMIN-> adminRepository.findByUsername(email)
                    .orElseThrow(AdminNotFoundException::new);
            default -> throw new InvalidRoleException();
        };

        // 최종적으로 인증 권한을 반환하게 된다.
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}
