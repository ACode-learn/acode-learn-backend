package gr.alexc.acodelearn.user.internal;

import gr.alexc.acodelearn.user.GlobalRole;
import gr.alexc.acodelearn.user.User;
import gr.alexc.acodelearn.user.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";

    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final Environment environment;

    @Transactional
    public UserView getOrCreateUser(Jwt jwt) {
        String externalId = jwt.getSubject();

        User user = findExistingUser(jwt)
                .map(existingUser -> syncUser(existingUser, jwt))
                .orElseGet(() -> {
                    try {
                        return createUser(jwt);
                    } catch (DataIntegrityViolationException e) {
                        log.info("User creation race condition for externalId: {}, retrying lookup", externalId);
                        return findExistingUser(jwt)
                                .map(existingUser -> syncUser(existingUser, jwt))
                                .orElseThrow(() -> e);
                    }
                });

        return toView(user);
    }

    private Optional<User> findExistingUser(Jwt jwt) {
        String externalId = jwt.getSubject();

        Optional<User> userByExternalId = userRepository.findByExternalId(externalId);
        if (userByExternalId.isPresent()) {
            return userByExternalId;
        }

        if (!isDevProfile()) {
            return Optional.empty();
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> {
                    log.info(
                            "Dev profile user relink by email: email={}, oldExternalId={}, newExternalId={}",
                            email,
                            user.getExternalId(),
                            externalId
                    );
                    user.setExternalId(externalId);
                    return user;
                });
    }

    private boolean isDevProfile() {
        return environment.acceptsProfiles(Profiles.of("dev"));
    }

    private User createUser(Jwt jwt) {
        User user = new User();
        user.setExternalId(jwt.getSubject());
        user.setUsername(jwt.getClaimAsString("preferred_username"));
        user.setEmail(jwt.getClaimAsString("email"));
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));
        user.setRoles(extractRoles(jwt));
        User saved = userRepository.save(user);
        events.publishEvent(new UserCreatedEvent(saved.getId(), saved.getExternalId(), saved.getEmail()));
        return saved;
    }

    private User syncUser(User user, Jwt jwt) {
        user.setUsername(jwt.getClaimAsString("preferred_username"));
        user.setEmail(jwt.getClaimAsString("email"));
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));
        Set<GlobalRole> jwtRoles = extractRoles(jwt);
        if (!jwtRoles.equals(user.getRoles())) {
            user.getRoles().clear();
            user.getRoles().addAll(jwtRoles);
        }
        return userRepository.save(user);
    }

    /**
     * Extracts the user's global roles from a Keycloak JWT, intersecting with
     * the locally known {@link GlobalRole} values. Reads {@code realm_access.roles}
     * and {@code resource_access.*.roles} so it stays consistent with the
     * authority converter.
     */
    @SuppressWarnings("unchecked")
    static Set<GlobalRole> extractRoles(Jwt jwt) {
        Set<String> raw = new HashSet<>();

        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        if (realmAccess != null && realmAccess.get(ROLES_CLAIM) instanceof Collection<?> rc) {
            ((Collection<String>) rc).forEach(raw::add);
        }
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess != null) {
            for (Object client : resourceAccess.values()) {
                if (client instanceof Map<?, ?> cm && cm.get(ROLES_CLAIM) instanceof Collection<?> rc) {
                    ((Collection<String>) rc).forEach(raw::add);
                }
            }
        }

        Set<GlobalRole> result = EnumSet.noneOf(GlobalRole.class);
        for (String r : raw) {
            if (r == null) continue;
            try {
                result.add(GlobalRole.valueOf(r.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Roles outside the GlobalRole enum (e.g. "offline_access") are intentionally skipped.
            }
        }
        return result;
    }

    private UserView toView(User user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRoles()
        );
    }

    /** Convenience for tests/migration tooling. */
    @Transactional(readOnly = true)
    public List<UserView> findAll() {
        return userRepository.findAll().stream().map(this::toView).toList();
    }
}
