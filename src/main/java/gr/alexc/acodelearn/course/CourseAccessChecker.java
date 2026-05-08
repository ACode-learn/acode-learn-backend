package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.course.internal.CourseEnrollmentRepository;
import gr.alexc.acodelearn.course.internal.CourseRepository;
import gr.alexc.acodelearn.user.User;
import gr.alexc.acodelearn.user.UserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Centralized course-level authorization. Exposed as Spring bean
 * {@code "courseAccess"} for use in {@code @PreAuthorize} SpEL, and called
 * directly from services for instance-level checks.
 *
 * <p>Rules of thumb (single ADMIN short-circuit):
 * <ul>
 *     <li>{@link #canView}: ADMIN, instructor, or active-enrolled student.</li>
 *     <li>{@link #canEdit}: ADMIN or instructor.</li>
 *     <li>{@link #canDelete}: ADMIN or owner.</li>
 *     <li>{@link #canManageInstructors}: ADMIN or owner.</li>
 *     <li>{@link #canManageEnrollments}: ADMIN or instructor.</li>
 * </ul>
 *
 * <p>Authentication-derived role checks read from
 * {@link Authentication#getAuthorities()} (i.e. the JWT), never from the local
 * role snapshot.
 */
@Component("courseAccess")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseAccessChecker {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final UserLookup userLookup;

    public boolean canView(Authentication authentication, Long courseId) {
        if (isAdmin(authentication)) return true;
        Course course = course(courseId).orElse(null);
        if (course == null) return false;
        Long uid = currentUserId(authentication).orElse(null);
        if (uid == null) return false;
        if (course.hasInstructor(uid)) return true;
        return enrollmentRepository.existsByCourseIdAndUserIdAndStatus(courseId, uid, CourseEnrollment.Status.ACTIVE);
    }

    public boolean canEdit(Authentication authentication, Long courseId) {
        if (isAdmin(authentication)) return true;
        Course course = course(courseId).orElse(null);
        if (course == null) return false;
        return currentUserId(authentication).map(course::hasInstructor).orElse(false);
    }

    public boolean canDelete(Authentication authentication, Long courseId) {
        if (isAdmin(authentication)) return true;
        Course course = course(courseId).orElse(null);
        if (course == null) return false;
        return currentUserId(authentication).map(course::isOwner).orElse(false);
    }

    public boolean canManageInstructors(Authentication authentication, Long courseId) {
        if (isAdmin(authentication)) return true;
        Course course = course(courseId).orElse(null);
        if (course == null) return false;
        return currentUserId(authentication).map(course::isOwner).orElse(false);
    }

    public boolean canManageEnrollments(Authentication authentication, Long courseId) {
        // Same rule as edit: any instructor (or admin) may enroll students.
        return canEdit(authentication, courseId);
    }

    // --- helpers ---

    public boolean isAdmin(Authentication authentication) {
        return hasAuthority(authentication, ROLE_ADMIN);
    }

    public boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authentication.getAuthorities() == null) return false;
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if (authority.equals(a.getAuthority())) return true;
        }
        return false;
    }

    private Optional<Long> currentUserId(Authentication authentication) {
        if (authentication == null) return Optional.empty();
        String username = authentication.getName();
        if (username == null || username.isBlank()) return Optional.empty();
        try {
            User user = userLookup.findByUsername(username);
            return Optional.ofNullable(user.getId());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Optional<Course> course(Long courseId) {
        if (courseId == null) return Optional.empty();
        return courseRepository.findById(courseId);
    }
}
