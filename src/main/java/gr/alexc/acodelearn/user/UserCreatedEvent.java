package gr.alexc.acodelearn.user;

/**
 * Published when a local {@link User} is first persisted (typically on the
 * user's first authenticated request). Other modules can listen to this event
 * to perform setup work that depends on the user existing locally — for
 * example, the course module activates pending email-based enrollments.
 */
public record UserCreatedEvent(Long userId, String externalId, String email) {}
