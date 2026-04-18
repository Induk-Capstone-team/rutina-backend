package com.rutina.rutinabackend.global.jwt;

import com.rutina.rutinabackend.global.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 모든 요청에 대해 한 번씩 실행(OncePerRequestFilter)
 * Authorization 헤더의 Bearer 토큰 검증
 * 유효한 경우 SecurityContext에 인증 정보를 저장
 *
 * 토큰이 없거나 유효하지 않으면 인증 없이 다음 필터로 넘김.
 * (PUBLIC_URLS는 SecurityConfig에서 permitAll로 처리)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Authorization 헤더가 없거나 Bearer 형식이 아니면 인증 없이 통과
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Bearer 이후의 토큰 값만 추출
        String token = header.substring(7);

        // 서명 및 만료 검증 후 SecurityContext에 인증 정보 저장
        if (jwtProvider.isValid(token)) {
            Long userId = jwtProvider.getUserId(token);
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            // 인증 객체 생성 (credentials는 이미 토큰으로 검증했으므로 null)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );

            // 부가 정보 설정(요청 IP, 세션 정보 등)
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // 이후 컨트롤러에서 @AuthenticationPrincipal로 꺼낼 수 있도록 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
