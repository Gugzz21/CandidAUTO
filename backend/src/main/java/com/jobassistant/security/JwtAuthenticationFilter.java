package com.jobassistant.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.shared.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Filtro de autenticação stateless:
 * 1) Valida tokens JWT assinados (Authorization: Bearer <token>).
 * 2) Bloqueia falsificação de identidade via cabeçalho X-User-Id em produção (retorna 403 Forbidden).
 * 3) Suporta sessão local segura apenas em loopback (127.0.0.1 / ::1) quando local-personal-mode=true.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper;
    private final boolean localPersonalMode;
    private final boolean allowTestUserHeader;

    public JwtAuthenticationFilter(
            AuthTokenService authTokenService,
            ObjectMapper objectMapper,
            @Value("${app.security.local-personal-mode:true}") boolean localPersonalMode,
            @Value("${app.security.allow-test-user-header:false}") boolean allowTestUserHeader
    ) {
        this.authTokenService = authTokenService;
        this.objectMapper = objectMapper;
        this.localPersonalMode = localPersonalMode;
        this.allowTestUserHeader = allowTestUserHeader;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String spoofedHeader = request.getHeader(USER_ID_HEADER);

        // 1. Autenticação via Token Assinado (JWT Bearer)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String rawToken = authHeader.substring(7).trim();
            Optional<AuthenticatedUserPrincipal> verified = authTokenService.verifyToken(rawToken);
            if (verified.isEmpty()) {
                writeJsonError(response, request, HttpStatus.UNAUTHORIZED, "Token de autenticação inválido ou expirado.");
                return;
            }

            AuthenticatedUserPrincipal principal = verified.get();
            if (spoofedHeader != null && !spoofedHeader.isBlank() && !allowTestUserHeader) {
                if (!principal.userId().toString().equalsIgnoreCase(spoofedHeader.trim())) {
                    writeJsonError(response, request, HttpStatus.FORBIDDEN,
                            "Tentativa de falsificação bloqueada: X-User-Id diverge do usuário autenticado no token.");
                    return;
                }
            }

            setAuthentication(principal);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Modo exclusivo de suítes de teste (@SpringBootTest com allow-test-user-header=true)
        if (allowTestUserHeader) {
            UUID testUserId = CurrentUserProvider.DEFAULT_USER_ID;
            if (spoofedHeader != null && !spoofedHeader.isBlank()) {
                try {
                    testUserId = UUID.fromString(spoofedHeader.trim());
                } catch (IllegalArgumentException e) {
                    writeJsonError(response, request, HttpStatus.BAD_REQUEST, "Cabeçalho X-User-Id inválido.");
                    return;
                }
            }
            setAuthentication(new AuthenticatedUserPrincipal(testUserId, "test@candidauto.local", "TEST_HEADER"));
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Bloquear qualquer tentativa não autenticada de injetar X-User-Id arbitrário
        if (spoofedHeader != null && !spoofedHeader.isBlank()) {
            writeJsonError(
                    response,
                    request,
                    HttpStatus.FORBIDDEN,
                    "Falsificação de identidade bloqueada: o cabeçalho X-User-Id não é aceito sem autenticação real."
            );
            return;
        }

        // 4. Modo Personal-First estritamente para loopback local (127.0.0.1 / ::1)
        if (localPersonalMode && isLoopbackAddress(request.getRemoteAddr())) {
            setAuthentication(new AuthenticatedUserPrincipal(
                    CurrentUserProvider.DEFAULT_USER_ID,
                    "candidato@jobassistant.local",
                    "LOCAL_LOOPBACK"
            ));
        }

        filterChain.doFilter(request, response);
    }

    public static boolean isLoopbackAddress(String addr) {
        if (addr == null) return false;
        return "127.0.0.1".equals(addr)
                || "0:0:0:0:0:0:0:1".equals(addr)
                || "::1".equals(addr)
                || "localhost".equalsIgnoreCase(addr);
    }

    private void setAuthentication(AuthenticatedUserPrincipal principal) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void writeJsonError(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse payload = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                List.of()
        );
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }
}
