package gr.alexc.acodelearn.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private static Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
    }

    private static Set<String> authorityNames(AbstractAuthenticationToken token) {
        return token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    void mapsRealmRolesToRoleAuthorities() {
        KeycloakJwtAuthenticationConverter converter =
                new KeycloakJwtAuthenticationConverter(null);

        Jwt jwt = baseJwt()
                .subject("sub-1")
                .claim("preferred_username", "alice")
                .claim("realm_access", Map.of("roles", List.of("TEACHER", "STUDENT")))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(authorityNames(token))
                .contains("ROLE_TEACHER", "ROLE_STUDENT");
        assertThat(token.getName()).isEqualTo("alice");
    }

    @Test
    void mapsResourceRolesWhenClientConfigured() {
        KeycloakJwtAuthenticationConverter converter =
                new KeycloakJwtAuthenticationConverter("acodelearn-backend");

        Jwt jwt = baseJwt()
                .subject("sub-2")
                .claim("preferred_username", "bob")
                .claim("resource_access", Map.of(
                        "acodelearn-backend", Map.of("roles", List.of("ADMIN")),
                        "other-client", Map.of("roles", List.of("UNRELATED"))
                ))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        Set<String> names = authorityNames(token);
        assertThat(names).contains("ROLE_ADMIN");
        assertThat(names).doesNotContain("ROLE_UNRELATED");
    }

    @Test
    void ignoresResourceRolesWhenClientNotConfigured() {
        KeycloakJwtAuthenticationConverter converter =
                new KeycloakJwtAuthenticationConverter(null);

        Jwt jwt = baseJwt()
                .subject("sub-3")
                .claim("preferred_username", "carol")
                .claim("resource_access", Map.of(
                        "acodelearn-backend", Map.of("roles", List.of("ADMIN"))
                ))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(authorityNames(token)).doesNotContain("ROLE_ADMIN");
    }

    @Test
    void usesPreferredUsernameAsPrincipal() {
        KeycloakJwtAuthenticationConverter converter =
                new KeycloakJwtAuthenticationConverter(null);

        Jwt jwt = baseJwt()
                .subject("subject-id")
                .claim("preferred_username", "dave")
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);
        assertThat(token.getName()).isEqualTo("dave");
    }

    @Test
    void fallsBackToSubjectWhenPreferredUsernameMissing() {
        KeycloakJwtAuthenticationConverter converter =
                new KeycloakJwtAuthenticationConverter(null);

        Jwt jwt = baseJwt()
                .subject("subject-fallback")
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);
        assertThat(token.getName()).isEqualTo("subject-fallback");
    }

    @Test
    void handlesMissingRealmAccessClaim() {
        KeycloakJwtAuthenticationConverter converter =
                new KeycloakJwtAuthenticationConverter("acodelearn-backend");

        Jwt jwt = baseJwt()
                .subject("subj")
                .claim("preferred_username", "eve")
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);
        // No ROLE_* authorities expected.
        assertThat(authorityNames(token))
                .noneMatch(a -> a.startsWith("ROLE_"));
    }

    @Test
    void uppercasesRoleNames() {
        KeycloakJwtAuthenticationConverter converter =
                new KeycloakJwtAuthenticationConverter(null);

        Jwt jwt = baseJwt()
                .subject("subj")
                .claim("preferred_username", "frank")
                .claim("realm_access", Map.of("roles", List.of("teacher")))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(authorityNames(token)).contains("ROLE_TEACHER");
    }
}
