package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.course.command.*;
import gr.alexc.acodelearn.course.query.projections.CourseEnrollmentView;
import gr.alexc.acodelearn.course.query.projections.CourseSectionView;
import gr.alexc.acodelearn.course.query.projections.CourseSummaryView;
import gr.alexc.acodelearn.resource.ResourceSummaryView;
import gr.alexc.acodelearn.resource.ResourceType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Course HTTP endpoints.
 *
 * <p>Authorization rules:
 * <ul>
 *     <li>Endpoint-level {@code hasRole(...)} checks come from the JWT (Keycloak
 *     realm roles, mapped via {@code KeycloakJwtAuthenticationConverter}).</li>
 *     <li>Instance-level checks (per-course) delegate to the
 *     {@link CourseAccessChecker} bean exposed as {@code @courseAccess}, which
 *     centralizes the ADMIN override.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    // --- Course ---

    @GetMapping("/user-courses")
    public ResponseEntity<List<CourseSummaryView>> getUserCourses(Authentication authentication) {
        return ResponseEntity.ok(courseService.getUserCourses(authentication.getName()));
    }

    @GetMapping("/instructor/owned-courses")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<List<CourseSummaryView>> getOwnedCourses(Authentication authentication) {
        return ResponseEntity.ok(courseService.getOwnedCourses(authentication.getName()));
    }

    @PostMapping("/course")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<CourseSummaryView> createCourse(
            @Valid @RequestBody CreateCourseCommand command,
            Authentication authentication
    ) {
        return ResponseEntity.ok(courseService.createCourse(command, authentication.getName()));
    }

    @PutMapping("/course/{courseId}")
    @PreAuthorize("@courseAccess.canEdit(authentication, #courseId)")
    public ResponseEntity<CourseSummaryView> updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseCommand command
    ) {
        return ResponseEntity.ok(courseService.updateCourse(command));
    }

    // --- Resources ---

    @GetMapping("/course/{courseId}/resources")
    @PreAuthorize("@courseAccess.canView(authentication, #courseId)")
    public ResponseEntity<List<ResourceSummaryView>> getCourseResources(
            @PathVariable Long courseId,
            @RequestParam(required = false) ResourceType type
    ) {
        return ResponseEntity.ok(courseService.getCourseResources(courseId, type));
    }

    // --- Sections ---

    @GetMapping("/course/{courseId}/sections")
    @PreAuthorize("@courseAccess.canView(authentication, #courseId)")
    public ResponseEntity<List<CourseSectionView>> getSections(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getSections(courseId));
    }

    @GetMapping("/course/{courseId}/sections/{sectionId}")
    @PreAuthorize("@courseAccess.canView(authentication, #courseId)")
    public ResponseEntity<CourseSectionView> getSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId
    ) {
        return ResponseEntity.ok(courseService.getSection(sectionId));
    }

    @PostMapping("/course/{courseId}/sections")
    @PreAuthorize("@courseAccess.canEdit(authentication, #courseId)")
    public ResponseEntity<CourseSectionView> createSection(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateCourseSectionCommand command
    ) {
        return ResponseEntity.ok(courseService.createSection(command));
    }

    @PutMapping("/course/{courseId}/sections/{sectionId}")
    @PreAuthorize("@courseAccess.canEdit(authentication, #courseId)")
    public ResponseEntity<CourseSectionView> updateSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateCourseSectionCommand command
    ) {
        return ResponseEntity.ok(courseService.updateSection(command));
    }

    @PutMapping("/course/{courseId}/sections/order")
    @PreAuthorize("@courseAccess.canEdit(authentication, #courseId)")
    public ResponseEntity<List<CourseSectionView>> reorderSections(
            @PathVariable Long courseId,
            @RequestBody Map<Long, Integer> orderMap
    ) {
        return ResponseEntity.ok(courseService.reorderSections(new ReorderSectionsCommand(courseId, orderMap)));
    }

    @DeleteMapping("/course/{courseId}/sections/{sectionId}")
    @PreAuthorize("@courseAccess.canEdit(authentication, #courseId)")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId
    ) {
        courseService.deleteSection(new DeleteCourseSectionCommand(sectionId, courseId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/course/{courseId}/sections/{sectionId}/resources")
    @PreAuthorize("@courseAccess.canEdit(authentication, #courseId)")
    public ResponseEntity<CourseSectionView> addResourcesToSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestBody List<Long> resourceIds
    ) {
        return ResponseEntity.ok(courseService.addResourcesToSection(
                new AddResourcesToSectionCommand(courseId, sectionId, resourceIds)
        ));
    }

    @DeleteMapping("/course/{courseId}/sections/{sectionId}/resources")
    @PreAuthorize("@courseAccess.canEdit(authentication, #courseId)")
    public ResponseEntity<CourseSectionView> removeResourcesFromSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestParam List<Long> resourceIds
    ) {
        return ResponseEntity.ok(courseService.removeResourcesFromSection(
                new RemoveResourcesFromSectionCommand(courseId, sectionId, resourceIds)
        ));
    }

    // --- Enrollments (instructor-driven) ---

    @GetMapping("/course/{courseId}/enrollments")
    @PreAuthorize("@courseAccess.canManageEnrollments(authentication, #courseId)")
    public ResponseEntity<List<CourseEnrollmentView>> listEnrollments(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.listEnrollments(courseId));
    }

    @GetMapping("/course/{courseId}/enrollments/pending")
    @PreAuthorize("@courseAccess.canManageEnrollments(authentication, #courseId)")
    public ResponseEntity<List<CourseEnrollmentView>> listPendingEnrollments(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.listPendingEnrollments(courseId));
    }

    @PostMapping("/course/{courseId}/enrollments/{userId}")
    @PreAuthorize("@courseAccess.canManageEnrollments(authentication, #courseId)")
    public ResponseEntity<CourseEnrollmentView> enrollStudent(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(enrollmentService.enrollStudent(courseId, userId));
    }

    @PostMapping("/course/{courseId}/enrollments/by-email")
    @PreAuthorize("@courseAccess.canManageEnrollments(authentication, #courseId)")
    public ResponseEntity<CourseEnrollmentView> enrollByEmail(
            @PathVariable Long courseId,
            @Valid @RequestBody EnrollByEmailCommand command
    ) {
        return ResponseEntity.ok(enrollmentService.enrollByEmail(courseId, command));
    }

    @DeleteMapping("/course/{courseId}/enrollments/{userId}")
    @PreAuthorize("@courseAccess.canManageEnrollments(authentication, #courseId)")
    public ResponseEntity<Void> removeEnrollment(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        enrollmentService.removeEnrollment(courseId, userId);
        return ResponseEntity.noContent().build();
    }

    // --- Instructor management (owner-only, ADMIN override) ---

    @PostMapping("/course/{courseId}/instructors/{userId}")
    @PreAuthorize("@courseAccess.canManageInstructors(authentication, #courseId)")
    public ResponseEntity<Void> addInstructor(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        enrollmentService.addInstructor(courseId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/course/{courseId}/instructors/{userId}")
    @PreAuthorize("@courseAccess.canManageInstructors(authentication, #courseId)")
    public ResponseEntity<Void> removeInstructor(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        enrollmentService.removeInstructor(courseId, userId);
        return ResponseEntity.noContent().build();
    }
}
