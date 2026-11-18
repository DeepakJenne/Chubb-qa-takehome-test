package com.example.demo.support;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Authenticates MockMvc requests as a Keycloak-style user.
 *
 * <p>Mirrors what {@code SecurityConfig} derives from a real token: subject = user id and the
 * realm role mapped to {@code ROLE_<ROLE>}. It bypasses signature validation, so tests about
 * token validity (expired, tampered) need a real Keycloak token instead.
 */
public final class TestAuth {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String CLAIMANT_ROLE = "CLAIMANT";

    private TestAuth() {
    }

    public static RequestPostProcessor asAdmin(UUID userId) {
        return authenticatedAs(userId, ADMIN_ROLE);
    }

    public static RequestPostProcessor asClaimant(UUID userId) {
        return authenticatedAs(userId, CLAIMANT_ROLE);
    }

    /** A valid token that carries no realm roles at all. */
    public static RequestPostProcessor withoutRoles(UUID userId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(token -> token.subject(userId.toString()))
                .authorities(List.of());
    }

    private static RequestPostProcessor authenticatedAs(UUID userId, String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(token -> token
                        .subject(userId.toString())
                        .claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority(ROLE_PREFIX + role));
    }
}
