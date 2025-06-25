package org.project.ttokttok.global.auth.security;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.global.auth.jwt.filter.JwtAuthenticationManager;
import org.project.ttokttok.global.auth.jwt.filter.TokenAuthenticationFilter;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.project.ttokttok.global.auth.security.SecurityWhiteList.ALLOW_URLS;
import static org.project.ttokttok.global.auth.security.SecurityWhiteList.SWAGGER_URLS;
import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final JwtAuthenticationManager jwtAuthenticationManager;

    @Value("${server.url}")
    private String serverUrl;

    // h2 db 접근용 security 비활성화
    @Bean
    public WebSecurityCustomizer customizer() {
        return web -> web.ignoring()
                .requestMatchers(toH2Console());
    }

    // 패스워드 인코더 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // 커스텀 토큰 필터 빈 등록
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider, jwtAuthenticationManager);
    }

    // todo: 추후에 보안 사항에 맞게 수정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(AbstractHttpConfigurer::disable) // 기본 http 비활성화
                .csrf(AbstractHttpConfigurer::disable) // csrf 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화
                .cors(cors -> cors.configurationSource(corsConfig())) // cors 설정 추가
                .sessionManagement(sessionManagement -> // jwt 방식이기에, 세션 무상태로 설정
                                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(request -> // 요청 권한 관련 설정
                        request
                                .requestMatchers(ALLOW_URLS.getEndPoints()).permitAll() // JWT를 가질 수 없는 요청은 허용
                                .requestMatchers(SWAGGER_URLS.getEndPoints()).permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN") // ADMIN 권한이 필요한 요청은 검증
                                // 일단 admin 제외, 다 열어둠
                                .anyRequest().permitAll()
                )
                //.exceptionHandling()
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class) // 필터 적용 전에 커스텀 필터 거치게 함
                .build();
    }

    //CORS 설정
    private CorsConfigurationSource corsConfig() {
        CorsConfiguration config = new CorsConfiguration();
        final String swaggerUrl = serverUrl + "/swagger-ui/index.html";

        config.setAllowedOriginPatterns(
                //todo: 나중에 프론트 배포 링크로 바꿀 것
                List.of("http://localhost:5173",
                        swaggerUrl)
        );
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); //허용 HTTP 메서드
        config.setAllowCredentials(true); // 쿠키 등 허용 설정
        config.setAllowedHeaders(List.of("*")); // todo: 추후 수정
        config.setMaxAge(3600L); // CORS 살아있는 시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}