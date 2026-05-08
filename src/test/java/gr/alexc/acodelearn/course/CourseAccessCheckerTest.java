package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.course.internal.CourseEnrollmentRepository;
import gr.alexc.acodelearn.course.internal.CourseRepository;
import gr.alexc.acodelearn.user.User;
import gr.alexc.acodelearn.user.UserLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAccessCheckerTest {

    @Mock CourseRepository courseRepository;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock UserLookup userLookup;

    @InjectMocks
    CourseAccessChecker checker;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10L);
        course.assignOwner(1L); // owner=1, instructorIds={1}
        course.addInstructor(2L); // instructor (non-owner)
        // student id = 3 (will be modeled via enrollment mock)

        lenient().when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
    }

    private Authentication auth(String username, String... roles) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(username, "n/a",
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
        token.setAuthenticated(true);
        return token;
    }

    private void mockUserId(String username, Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        when(userLookup.findByUsername(username)).thenReturn(u);
    }

    // --- ADMIN short-circuit ---

    @Test
    void adminCanDoEverythingWithoutDbLookup() {
        Authentication a = auth("admin", "ROLE_ADMIN");
        assertThat(checker.canView(a, 10L)).isTrue();
        assertThat(checker.canEdit(a, 10L)).isTrue();
        assertThat(checker.canDelete(a, 10L)).isTrue();
        assertThat(checker.canManageInstructors(a, 10L)).isTrue();
        assertThat(checker.canManageEnrollments(a, 10L)).isTrue();
    }

    // --- canView ---

    @Test
    void instructorCanView() {
        mockUserId("teacher", 1L);
        assertThat(checker.canView(auth("teacher", "ROLE_TEACHER"), 10L)).isTrue();
    }

    @Test
    void enrolledActiveStudentCanView() {
        mockUserId("student", 3L);
        when(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(10L, 3L, CourseEnrollment.Status.ACTIVE))
                .thenReturn(true);
        assertThat(checker.canView(auth("student", "ROLE_STUDENT"), 10L)).isTrue();
    }

    @Test
    void nonInstructorNonEnrolledCannotView() {
        mockUserId("stranger", 99L);
        when(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(10L, 99L, CourseEnrollment.Status.ACTIVE))
                .thenReturn(false);
        assertThat(checker.canView(auth("stranger", "ROLE_STUDENT"), 10L)).isFalse();
    }

    @Test
    void unknownCourseDeniesView() {
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());
        assertThat(checker.canView(auth("anyone", "ROLE_TEACHER"), 404L)).isFalse();
    }

    // --- canEdit ---

    @Test
    void instructorCanEdit() {
        mockUserId("teacher2", 2L);
        assertThat(checker.canEdit(auth("teacher2", "ROLE_TEACHER"), 10L)).isTrue();
    }

    @Test
    void nonInstructorTeacherCannotEdit() {
        mockUserId("otherteacher", 77L);
        assertThat(checker.canEdit(auth("otherteacher", "ROLE_TEACHER"), 10L)).isFalse();
    }

    @Test
    void enrolledStudentCannotEdit() {
        mockUserId("student", 3L);
        // Even if active enrollment, edit requires instructor.
        assertThat(checker.canEdit(auth("student", "ROLE_STUDENT"), 10L)).isFalse();
    }

    // --- canDelete / canManageInstructors (owner-only) ---

    @Test
    void ownerCanDelete() {
        mockUserId("owner", 1L);
        assertThat(checker.canDelete(auth("owner", "ROLE_TEACHER"), 10L)).isTrue();
    }

    @Test
    void nonOwnerInstructorCannotDelete() {
        mockUserId("teacher2", 2L);
        assertThat(checker.canDelete(auth("teacher2", "ROLE_TEACHER"), 10L)).isFalse();
    }

    @Test
    void ownerCanManageInstructors() {
        mockUserId("owner", 1L);
        assertThat(checker.canManageInstructors(auth("owner", "ROLE_TEACHER"), 10L)).isTrue();
    }

    @Test
    void nonOwnerInstructorCannotManageInstructors() {
        mockUserId("teacher2", 2L);
        assertThat(checker.canManageInstructors(auth("teacher2", "ROLE_TEACHER"), 10L)).isFalse();
    }

    // --- canManageEnrollments (any instructor) ---

    @Test
    void instructorCanManageEnrollments() {
        mockUserId("teacher2", 2L);
        assertThat(checker.canManageEnrollments(auth("teacher2", "ROLE_TEACHER"), 10L)).isTrue();
    }

    @Test
    void studentCannotManageEnrollments() {
        mockUserId("student", 3L);
        assertThat(checker.canManageEnrollments(auth("student", "ROLE_STUDENT"), 10L)).isFalse();
    }

    // --- guardrails ---

    @Test
    void nullAuthenticationDeniesEverything() {
        assertThat(checker.canView(null, 10L)).isFalse();
        assertThat(checker.canEdit(null, 10L)).isFalse();
        assertThat(checker.canDelete(null, 10L)).isFalse();
        assertThat(checker.canManageInstructors(null, 10L)).isFalse();
    }

    @Test
    void unknownPrincipalUsernameDeniesAccess() {
        when(userLookup.findByUsername("ghost"))
                .thenThrow(new RuntimeException("not found"));
        assertThat(checker.canView(auth("ghost", "ROLE_STUDENT"), 10L)).isFalse();
    }

    @Test
    void isAdminReadsJwtAuthorities() {
        assertThat(checker.isAdmin(auth("a", "ROLE_ADMIN"))).isTrue();
        assertThat(checker.isAdmin(auth("a", "ROLE_TEACHER"))).isFalse();
        assertThat(checker.isAdmin(null)).isFalse();
    }
}
