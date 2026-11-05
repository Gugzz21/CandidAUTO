package com.jobassistant.security;

import com.jobassistant.profile.domain.UserEntity;
import com.jobassistant.profile.repository.UserRepository;
import com.jobassistant.shared.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Resolve a identidade do usuário exclusivamente a partir do contexto autenticado do Spring Security
 * (nunca confiando diretamente em cabeçalhos não autenticados enviados pelo cliente).
 */
@Component
public class CurrentUserProvider {

    public static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado no contexto da requisição.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal authUser) {
            return ensureUserExists(authUser.userId());
        }
        throw new AccessDeniedException("Principal de autenticação inválido.");
    }

    @Transactional
    public UUID ensureUserExists(UUID userId) {
        if (userId == null) {
            throw new BadRequestException("Identificador de usuário não pode ser nulo.");
        }
        if (!userRepository.existsById(userId)) {
            String email = userId.equals(DEFAULT_USER_ID)
                    ? "candidato@jobassistant.local"
                    : "user-" + userId + "@jobassistant.local";
            String name = userId.equals(DEFAULT_USER_ID)
                    ? "Candidato Principal (Personal First)"
                    : "Usuário " + userId.toString().substring(0, 8);
            userRepository.save(new UserEntity(userId, name, email, Instant.now()));
        }
        return userId;
    }
}
