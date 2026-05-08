package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.course.command.EnrollByEmailCommand;
import gr.alexc.acodelearn.course.internal.CourseEnrollmentRepository;
import gr.alexc.acodelearn.course.internal.CourseRepository;
import gr.alexc.acodelearn.course.query.projections.CourseEnrollmentView;
import gr.alexc.acodelearn.shared.ContentNotFoundException;
import gr.alexc.acodelearn.shared.UserNotAllowedException;
import gr.alexc.acodelearn.user.GlobalRole;
import gr.alexc.acodelearn.user.User;
import gr.alexc.acodelearn.user.UserLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock UserLookup userLookup;

    @InjectMocks
    EnrollmentService service;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10L);
        course.assignOwner(1L);
    }

    private User user(Long id, String email, GlobalRole... roles) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRoles(roles.length == 0
                ? EnumSet.noneOf(GlobalRole.class)
                : EnumSet.copyOf(List.of(roles)));
        return u;
    }

    // --- enrollStudent ---

    @Test
    void enrollStudent_createsActiveEnrollment() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userLookup.findById(3L)).thenReturn(Optional.of(user(3L, "s@x.com", GlobalRole.STUDENT)));
        when(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(10L, 3L, CourseEnrollment.Status.ACTIVE))
                .thenReturn(false);
        when(enrollmentRepository.save(any(CourseEnrollment.class)))
                .thenAnswer(inv -> {
                    CourseEnrollment e = inv.getArgument(0);
                    e.setId(100L);
                    return e;
                });

        CourseEnrollmentView v = service.enrollStudent(10L, 3L);

        assertThat(v.status()).isEqualTo(CourseEnrollment.Status.ACTIVE);
        assertThat(v.userId()).isEqualTo(3L);
        assertThat(v.courseId()).isEqualTo(10L);

        ArgumentCaptor<CourseEnrollment> captor = ArgumentCaptor.forClass(CourseEnrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CourseEnrollment.Status.ACTIVE);
        assertThat(captor.getValue().getEmail()).isEqualTo("s@x.com");
    }

    @Test
    void enrollStudent_idempotentWhenAlreadyActive() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userLookup.findById(3L)).thenReturn(Optional.of(user(3L, "s@x.com", GlobalRole.STUDENT)));
        when(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(10L, 3L, CourseEnrollment.Status.ACTIVE))
                .thenReturn(true);
        CourseEnrollment existing = CourseEnrollment.active(10L, 3L, "s@x.com");
        existing.setId(7L);
        when(enrollmentRepository.findByCourseIdAndUserId(10L, 3L)).thenReturn(Optional.of(existing));

        CourseEnrollmentView v = service.enrollStudent(10L, 3L);

        assertThat(v.id()).isEqualTo(7L);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollStudent_unknownCourseThrows() {
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enrollStudent(404L, 3L))
                .isInstanceOf(ContentNotFoundException.class);
    }

    @Test
    void enrollStudent_unknownUserThrows() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userLookup.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enrollStudent(10L, 99L))
                .isInstanceOf(ContentNotFoundException.class);
    }

    // --- enrollByEmail ---

    @Test
    void enrollByEmail_existingUser_createsActive() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userLookup.findByEmail("a@b.com"))
                .thenReturn(Optional.of(user(5L, "a@b.com", GlobalRole.STUDENT)));
        when(userLookup.findById(5L))
                .thenReturn(Optional.of(user(5L, "a@b.com", GlobalRole.STUDENT)));
        when(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(10L, 5L, CourseEnrollment.Status.ACTIVE))
                .thenReturn(false);
        when(enrollmentRepository.save(any(CourseEnrollment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CourseEnrollmentView v = service.enrollByEmail(10L, new EnrollByEmailCommand("a@b.com"));

        assertThat(v.status()).isEqualTo(CourseEnrollment.Status.ACTIVE);
        assertThat(v.userId()).isEqualTo(5L);
    }

    @Test
    void enrollByEmail_unknownUser_createsPending() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userLookup.findByEmail("new@x.com")).thenReturn(Optional.empty());
        when(enrollmentRepository.existsByCourseIdAndEmailIgnoreCaseAndStatus(
                10L, "new@x.com", CourseEnrollment.Status.PENDING)).thenReturn(false);
        when(enrollmentRepository.save(any(CourseEnrollment.class)))
                .thenAnswer(inv -> {
                    CourseEnrollment e = inv.getArgument(0);
                    e.setId(50L);
                    return e;
                });

        CourseEnrollmentView v = service.enrollByEmail(10L, new EnrollByEmailCommand("new@x.com"));

        assertThat(v.status()).isEqualTo(CourseEnrollment.Status.PENDING);
        assertThat(v.email()).isEqualTo("new@x.com");
        assertThat(v.userId()).isNull();
    }

    @Test
    void enrollByEmail_alreadyPending_returnsExistingWithoutCreating() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userLookup.findByEmail("pending@x.com")).thenReturn(Optional.empty());
        when(enrollmentRepository.existsByCourseIdAndEmailIgnoreCaseAndStatus(
                10L, "pending@x.com", CourseEnrollment.Status.PENDING)).thenReturn(true);
        CourseEnrollment existing = CourseEnrollment.pending(10L, "pending@x.com");
        existing.setId(42L);
        when(enrollmentRepository.findByCourseIdAndStatus(10L, CourseEnrollment.Status.PENDING))
                .thenReturn(List.of(existing));

        CourseEnrollmentView v = service.enrollByEmail(10L, new EnrollByEmailCommand("pending@x.com"));

        assertThat(v.id()).isEqualTo(42L);
        verify(enrollmentRepository, never()).save(any());
    }

    // --- activatePendingEnrollments ---

    @Test
    void activatePendingEnrollments_marksAllMatchingPendingActive() {
        CourseEnrollment p1 = CourseEnrollment.pending(10L, "u@x.com");
        CourseEnrollment p2 = CourseEnrollment.pending(11L, "U@X.COM");
        when(enrollmentRepository.findByEmailIgnoreCaseAndStatus("u@x.com", CourseEnrollment.Status.PENDING))
                .thenReturn(List.of(p1, p2));

        int activated = service.activatePendingEnrollments(7L, "u@x.com");

        assertThat(activated).isEqualTo(2);
        assertThat(p1.getStatus()).isEqualTo(CourseEnrollment.Status.ACTIVE);
        assertThat(p1.getUserId()).isEqualTo(7L);
        assertThat(p2.getStatus()).isEqualTo(CourseEnrollment.Status.ACTIVE);
        verify(enrollmentRepository, times(1)).saveAll(any());
    }

    @Test
    void activatePendingEnrollments_blankEmail_noOp() {
        assertThat(service.activatePendingEnrollments(7L, "")).isZero();
        assertThat(service.activatePendingEnrollments(7L, null)).isZero();
        assertThat(service.activatePendingEnrollments(null, "x@x.com")).isZero();
        verify(enrollmentRepository, never()).findByEmailIgnoreCaseAndStatus(any(), any());
    }

    @Test
    void activatePendingEnrollments_noMatches_skipsSave() {
        when(enrollmentRepository.findByEmailIgnoreCaseAndStatus("z@x.com", CourseEnrollment.Status.PENDING))
                .thenReturn(List.of());

        assertThat(service.activatePendingEnrollments(1L, "z@x.com")).isZero();
        verify(enrollmentRepository, never()).saveAll(any());
    }

    // --- removeEnrollment ---

    @Test
    void removeEnrollment_delegatesToRepository() {
        service.removeEnrollment(10L, 3L);
        verify(enrollmentRepository).deleteByCourseIdAndUserId(10L, 3L);
    }

    // --- addInstructor / removeInstructor ---

    @Test
    void addInstructor_requiresTeacherOrAdminRole() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userLookup.findById(99L))
                .thenReturn(Optional.of(user(99L, "s@x.com", GlobalRole.STUDENT)));

        assertThatThrownBy(() -> service.addInstructor(10L, 99L))
                .isInstanceOf(UserNotAllowedException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void addInstructor_acceptsTeacher() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userLookup.findById(8L))
                .thenReturn(Optional.of(user(8L, "t@x.com", GlobalRole.TEACHER)));

        service.addInstructor(10L, 8L);

        assertThat(course.getInstructorIds()).contains(8L);
        verify(courseRepository).save(course);
    }

    @Test
    void addInstructor_acceptsAdmin() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userLookup.findById(9L))
                .thenReturn(Optional.of(user(9L, "a@x.com", GlobalRole.ADMIN)));

        service.addInstructor(10L, 9L);
        assertThat(course.getInstructorIds()).contains(9L);
    }

    @Test
    void removeInstructor_delegatesAndPersists() {
        course.addInstructor(2L);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        service.removeInstructor(10L, 2L);

        assertThat(course.getInstructorIds()).doesNotContain(2L);
        verify(courseRepository).save(course);
    }

    @Test
    void removeInstructor_cannotRemoveOwner() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.removeInstructor(10L, 1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
