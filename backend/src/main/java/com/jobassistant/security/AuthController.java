package com.jobassistant.security;

import com.jobassistant.profile.domain.UserEntity;
import com.jobassistant.profile.repository.UserRepository;
import com.jobassistant.shared.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final CurrentUserProvider currentUserProvider;
    private final boolean localPersonalMode;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService,
            CurrentUserProvider currentUserProvider,
            @Value("${app.security.local-personal-mode:true}") boolean localPersonalMode
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.currentUserProvider = currentUserProvider;
        this.localPersonalMode = localPersonalMode;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        String cleanEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new BadRequestException("Já existe uma conta cadastrada com este e-mail.");
        }

        UUID userId = UUID.randomUUID();
        String hash = passwordEncoder.encode(request.password());
        UserEntity user = userRepository.save(new UserEntity(
                userId,
                request.name().trim(),
                cleanEmail,
                hash,
                Instant.now()
        ));

        String token = authTokenService.issueToken(user.getId(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponseDto(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        ));
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        String cleanEmail = request.email().trim().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByEmailIgnoreCase(cleanEmail).orElse(null);
        if (user == null || user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciais inválidas."));
        }

        String token = authTokenService.issueToken(user.getId(), user.getEmail());
        return ResponseEntity.ok(new AuthResponseDto(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        ));
    }

    @PostMapping("/local-session")
    @Transactional
    public ResponseEntity<?> createLocalPersonalSession(HttpServletRequest request) {
        if (!localPersonalMode || !JwtAuthenticationFilter.isLoopbackAddress(request.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Sessão local automática disponível apenas em ambiente local (loopback)."));
        }

        UUID defaultId = currentUserProvider.ensureUserExists(CurrentUserProvider.DEFAULT_USER_ID);
        UserEntity user = userRepository.findById(defaultId).orElseThrow();
        String token = authTokenService.issueToken(user.getId(), user.getEmail());
        return ResponseEntity.ok(new AuthResponseDto(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        ));
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> me() {
        UUID userId = currentUserProvider.getCurrentUserId();
        UserEntity user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail()
        ));
    }

    public record RegisterRequestDto(
            @NotBlank @Size(min = 2, max = 180) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 120) String password
    ) {
    }

    public record LoginRequestDto(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponseDto(
            String token,
            UUID userId,
            String name,
            String email
    ) {
    }
}
