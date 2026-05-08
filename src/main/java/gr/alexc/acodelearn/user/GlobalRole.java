package gr.alexc.acodelearn.user;

/**
 * Global identity-level roles, mirrored from Keycloak realm roles.
 *
 * <p>The local copy stored on {@link User} is a snapshot used <b>only</b> for
 * querying / filtering / reporting (e.g. "list all teachers"). It is
 * <b>never</b> the source of truth for authorization decisions on the current
 * request — those decisions must use {@code Authentication#getAuthorities()},
 * which is derived from the JWT.
 */
public enum GlobalRole {
    STUDENT,
    TEACHER,
    ADMIN
}
