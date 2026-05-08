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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Course-relationship management: enrollments (instructor-driven, with optional
 * by-email pending invite) and instructor management.
 *
 * <p>Authorization is performed at the controller boundary via
 * {@link CourseAccessChecker} / {@code @PreAuthorize}, so this service does not
 * re-check permissions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final UserLookup userLookup;

    /** Enroll an existing local user (by id) as an active student. */
    @Transactional
    public CourseEnrollmentView enrollStudent(Long courseId, Long userId) {
        Course course = requireCourse(courseId);
        User user = userLookup.findById(userId)
                .orElseThrow(() -> new ContentNotFoundException("User not found: " + userId));

        if (enrollmentRepository.existsByCourseIdAndUserIdAndStatus(course.getId(), user.getId(), CourseEnrollment.Status.ACTIVE)) {
            return enrollmentRepository.findByCourseIdAndUserId(course.getId(), user.getId())
                    .map(CourseEnrollmentView::of)
                    .orElseThrow();
        }
        CourseEnrollment enrollment = CourseEnrollment.active(course.getId(), user.getId(), user.getEmail());
        return CourseEnrollmentView.of(enrollmentRepository.save(enrollment));
    }

    /**
     * Enroll by email. If a local user with that email exists the enrollment is
     * created in {@link CourseEnrollment.Status#ACTIVE}. Otherwise it is stored
     * as {@link CourseEnrollment.Status#PENDING} and will be activated
     * automatically on the user's next login (see
     * {@link #activatePendingEnrollments(Long, String)}).
     */
    @Transactional
    public CourseEnrollmentView enrollByEmail(Long courseId, EnrollByEmailCommand command) {
        Course course = requireCourse(courseId);
        String email = command.email().trim();

        Optional<User> existing = userLookup.findByEmail(email);
        if (existing.isPresent()) {
            return enrollStudent(course.getId(), existing.get().getId());
        }

        if (enrollmentRepository.existsByCourseIdAndEmailIgnoreCaseAndStatus(course.getId(), email, CourseEnrollment.Status.PENDING)) {
            return enrollmentRepository.findByCourseIdAndStatus(course.getId(), CourseEnrollment.Status.PENDING).stream()
                    .filter(e -> email.equalsIgnoreCase(e.getEmail()))
                    .findFirst()
                    .map(CourseEnrollmentView::of)
                    .orElseThrow();
        }
        CourseEnrollment pending = CourseEnrollment.pending(course.getId(), email);
        return CourseEnrollmentView.of(enrollmentRepository.save(pending));
    }

    /**
     * Called when a user is created/synced. Looks up any pending enrollments
     * matching the user's email and converts them to ACTIVE.
     */
    @Transactional
    public int activatePendingEnrollments(Long userId, String email) {
        if (email == null || email.isBlank() || userId == null) {
            return 0;
        }
        List<CourseEnrollment> pending = enrollmentRepository
                .findByEmailIgnoreCaseAndStatus(email, CourseEnrollment.Status.PENDING);
        pending.forEach(e -> e.activate(userId));
        if (!pending.isEmpty()) {
            enrollmentRepository.saveAll(pending);
            log.info("Activated {} pending enrollment(s) for user id={} email={}", pending.size(), userId, email);
        }
        return pending.size();
    }

    @Transactional
    public void removeEnrollment(Long courseId, Long userId) {
        enrollmentRepository.deleteByCourseIdAndUserId(courseId, userId);
    }

    @Transactional(readOnly = true)
    public List<CourseEnrollmentView> listEnrollments(Long courseId) {
        return enrollmentRepository.findByCourseIdAndStatus(courseId, CourseEnrollment.Status.ACTIVE).stream()
                .map(CourseEnrollmentView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseEnrollmentView> listPendingEnrollments(Long courseId) {
        return enrollmentRepository.findByCourseIdAndStatus(courseId, CourseEnrollment.Status.PENDING).stream()
                .map(CourseEnrollmentView::of)
                .toList();
    }

    // --- Instructor management ---

    @Transactional
    public void addInstructor(Long courseId, Long userId) {
        Course course = requireCourse(courseId);
        User user = userLookup.findById(userId)
                .orElseThrow(() -> new ContentNotFoundException("User not found: " + userId));
        if (!user.getRoles().contains(GlobalRole.TEACHER) && !user.getRoles().contains(GlobalRole.ADMIN)) {
            throw new UserNotAllowedException("Only users with TEACHER or ADMIN role can be added as instructors");
        }
        course.addInstructor(user.getId());
        courseRepository.save(course);
    }

    @Transactional
    public void removeInstructor(Long courseId, Long userId) {
        Course course = requireCourse(courseId);
        course.removeInstructor(userId);
        courseRepository.save(course);
    }

    private Course requireCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ContentNotFoundException("Course not found: " + courseId));
    }
}
