package com.jobassistant.security;

import com.jobassistant.profile.domain.UserEntity;
import com.jobassistant.profile.repository.UserRepository;
import com.jobassistant.shared.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

@Component
public class CurrentUserProvider {

    public static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UUID getCurrentUserId() {
        UUID resolvedId = resolveFromRequestHeader();
        if (!userRepository.existsById(resolvedId)) {
            String email = resolvedId.equals(DEFAULT_USER_ID)
                    ? "candidato@jobassistant.local"
                    : "user-" + resolvedId + "@jobassistant.local";
            String name = resolvedId.equals(DEFAULT_USER_ID)
                    ? "Candidato Principal (Personal First)"
                    : "Usuário " + resolvedId.toString().substring(0, 8);
            userRepository.save(new UserEntity(resolvedId, name, email, Instant.now()));
        }
        return resolvedId;
    }

    private UUID resolveFromRequestHeader() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return DEFAULT_USER_ID;
        }
        HttpServletRequest request = attrs.getRequest();
        String header = request.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            return DEFAULT_USER_ID;
        }
        try {
            return UUID.fromString(header.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Cabeçalho X-User-Id inválido: deve ser um UUID válido.");
        }
    }
}
