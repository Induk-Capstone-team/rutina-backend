package com.rutina.rutinabackend.global.config;

import com.rutina.rutinabackend.global.jwt.JwtAuthenticationFilter;
import com.rutina.rutinabackend.global.oauth2.CustomOAuth2UserService;
import com.rutina.rutinabackend.global.oauth2.OAuth2FailureHandler;
import com.rutina.rutinabackend.global.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    // 인증 없이 접근 가능한 엔드포인트
    private static final String[] PUBLIC_URLS = {
            "/",
            "/index.html",

            "/official",
            "/official/",
            "/official/**",

            "/favicon.ico",

            "/api/v1/auth/**",
            "/api/v1/users/password-reset",
            "/api/ai/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-config",
            "/error",
            "/login/oauth2/**",    // 카카오/네이버 콜백 수신
            "/oauth2/**",          // 소셜 로그인 시작 요청
            "/images/**",          // 이미지
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)                          // JWT 방식이므로 CSRF 불필요
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 미사용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                // OAuth2 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository( // STATELESS 세션 정책에서도 OAuth2 state 파라미터 검증을 위해
                                                                 // 인가 요청 정보를 세션에 임시 저장 (로그인 완료 후 세션 소멸)
                                        new HttpSessionOAuth2AuthorizationRequestRepository() // OAuth2 state만 세션 사용
                                )
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                // UsernamePasswordAuthenticationFilter 앞에 JWT 필터 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 단방향 해시, 레인보우 테이블 공격 방어
    }
}
