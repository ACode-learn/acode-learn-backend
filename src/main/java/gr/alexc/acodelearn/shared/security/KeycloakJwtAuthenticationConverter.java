package gr.alexc.acodelearn.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts a Keycloak-issued JWT into a Spring {@link JwtAuthenticationToken}
 * whose authorities include:
 * <ul>
 *     <li>The default {@code SCOPE_*} authorities from {@code scope}/{@code scp}</li>
 *     <li>{@code ROLE_*} authorities extracted from the Keycloak realm roles
 *         claim {@code realm_access.roles}</li>
 *     <li>{@code ROLE_*} authorities extracted from this client's resource
 *         roles in {@code resource_access.<client-id>.roles} (when
 *         {@code resourceClientId} is configured)</li>
 * </ul>
 *
 * <p>The principal name is taken from {@code preferred_username} (with
 * fallback to {@code sub}) so that controllers seeing
 * {@link org.springframework.security.core.Authentication#getName()} get the
 * Keycloak username consistently with the rest of the codebase.
 */
public class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String PRINCIPAL_CLAIM = "preferred_username";

    private final JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
    private final String resourceClientId;

    public KeycloakJwtAuthenticationConverter(String resourceClientId) {
        this.resourceClientId = resourceClientId;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = Stream.of(
                        scopesConverter.convert(jwt).stream(),
                        realmRoles(jwt).stream(),
                        resourceRoles(jwt).stream()
                )
                .flatMap(s -> s)
                .collect(Collectors.toCollection(HashSet::new));

        String principalName = jwt.getClaimAsString(PRINCIPAL_CLAIM);
        if (principalName == null || principalName.isBlank()) {
            principalName = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        Object realmAccessClaim = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return List.of();
        }
        Object roles = realmAccess.get(ROLES_CLAIM);
        if (!(roles instanceof Collection<?> rolesCol)) {
            return List.of();
        }
        return ((Collection<String>) rolesCol).stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + r.toUpperCase()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> resourceRoles(Jwt jwt) {
        if (resourceClientId == null || resourceClientId.isBlank()) {
            return List.of();
        }
        Object resourceAccessClaim = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (!(resourceAccessClaim instanceof Map<?, ?> resourceAccess)) {
            return List.of();
        }
        Object client = resourceAccess.get(resourceClientId);
        if (!(client instanceof Map<?, ?> clientMap)) {
            return List.of();
        }
        Object roles = clientMap.get(ROLES_CLAIM);
        if (!(roles instanceof Collection<?> rolesCol)) {
            return List.of();
        }
        return ((Collection<String>) rolesCol).stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + r.toUpperCase()))
                .toList();
    }
}
