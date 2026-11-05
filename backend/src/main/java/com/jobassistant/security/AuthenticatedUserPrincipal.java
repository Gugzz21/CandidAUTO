package com.jobassistant.security;

import java.util.UUID;

public record AuthenticatedUserPrincipal(
        UUID userId,
        String email,
        String authMethod
) {
}
