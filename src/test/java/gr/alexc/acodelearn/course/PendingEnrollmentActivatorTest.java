package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.user.UserCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PendingEnrollmentActivatorTest {

    @Mock EnrollmentService enrollmentService;

    @InjectMocks
    PendingEnrollmentActivator activator;

    @Test
    void onUserCreated_activatesPendingEnrollmentsForUserEmail() {
        UserCreatedEvent event = new UserCreatedEvent(42L, "ext-1", "newuser@example.com");

        activator.on(event);

        verify(enrollmentService).activatePendingEnrollments(42L, "newuser@example.com");
    }

    @Test
    void onUserCreated_passesNullEmailThroughToService() {
        UserCreatedEvent event = new UserCreatedEvent(7L, "ext-2", null);

        activator.on(event);

        // Service handles null/blank internally; activator passes claims as-is.
        verify(enrollmentService).activatePendingEnrollments(7L, null);
    }
}
