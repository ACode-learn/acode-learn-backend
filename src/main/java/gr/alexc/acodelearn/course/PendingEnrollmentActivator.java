package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.user.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * On user creation, activates any pending email-based enrollments for the
 * newly registered user. Decoupled from the User module via a Modulith
 * application event.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingEnrollmentActivator {

    private final EnrollmentService enrollmentService;

    @ApplicationModuleListener
    void on(UserCreatedEvent event) {
        int activated = enrollmentService.activatePendingEnrollments(event.userId(), event.email());
        if (activated > 0) {
            log.info("Activated {} pending enrollment(s) for new user id={}", activated, event.userId());
        }
    }
}
