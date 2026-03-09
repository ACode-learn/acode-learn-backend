package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.course.command.*;
import gr.alexc.acodelearn.course.query.projections.CourseSectionView;
import gr.alexc.acodelearn.course.query.projections.CourseSummaryView;
import gr.alexc.acodelearn.resource.ResourceSummaryView;
import gr.alexc.acodelearn.resource.ResourceType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // --- Course ---

    @GetMapping("/user-courses")
    public ResponseEntity<List<CourseSummaryView>> getUserCourses(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(courseService.getUserCourses(jwt.getSubject()));
    }

    @GetMapping("/instructor/owned-courses")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<CourseSummaryView>> getOwnedCourses(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(courseService.getOwnedCourses(jwt.getSubject()));
    }

    @PutMapping("/course/{courseId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseSummaryView> updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseCommand command,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, command, jwt.getSubject()));
    }

    // --- Resources ---

    @GetMapping("/course/{courseId}/resources")
    public ResponseEntity<List<ResourceSummaryView>> getCourseResources(
            @PathVariable Long courseId,
            @RequestParam(required = false) ResourceType type,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(courseService.getCourseResources(courseId, type, jwt.getSubject()));
    }

    // --- Sections ---

    @GetMapping("/course/{courseId}/sections")
    public ResponseEntity<List<CourseSectionView>> getSections(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(courseService.getSections(courseId, jwt.getSubject()));
    }

    @GetMapping("/course/{courseId}/sections/{sectionId}")
    public ResponseEntity<CourseSectionView> getSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(courseService.getSection(courseId, sectionId, jwt.getSubject()));
    }

    @PostMapping("/course/{courseId}/sections")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseSectionView> createSection(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateCourseSectionCommand command,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(courseService.createSection(command, jwt.getSubject()));
    }

    @PutMapping("/course/{courseId}/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseSectionView> updateSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateCourseSectionCommand command,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(courseService.updateSection(command, jwt.getSubject()));
    }

    @PutMapping("/course/{courseId}/sections/order")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<CourseSectionView>> reorderSections(
            @PathVariable Long courseId,
            @RequestBody Map<Long, Integer> orderMap,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(courseService.reorderSections(new ReorderSectionsCommand(courseId, orderMap), jwt.getSubject()));
    }

    @DeleteMapping("/course/{courseId}/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        courseService.deleteSection(new DeleteCourseSectionCommand(sectionId, courseId), jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/course/{courseId}/sections/{sectionId}/resources")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseSectionView> addResourcesToSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestBody List<Long> resourceIds,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(courseService.addResourcesToSection(
                new AddResourcesToSectionCommand(courseId, sectionId, resourceIds),
                jwt.getSubject()
        ));
    }

    @DeleteMapping("/course/{courseId}/sections/{sectionId}/resources")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseSectionView> removeResourcesFromSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestParam List<Long> resourceIds,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(courseService.removeResourcesFromSection(
                new RemoveResourcesFromSectionCommand(courseId, sectionId, resourceIds),
                jwt.getSubject()
        ));
    }
}
