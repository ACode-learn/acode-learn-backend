package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.course.command.*;
import gr.alexc.acodelearn.course.query.CourseQueryHandler;
import gr.alexc.acodelearn.course.query.projections.CourseSectionView;
import gr.alexc.acodelearn.course.query.projections.CourseSummaryView;
import gr.alexc.acodelearn.resource.Resource;
import gr.alexc.acodelearn.resource.ResourceLookup;
import gr.alexc.acodelearn.resource.ResourceSummaryView;
import gr.alexc.acodelearn.resource.ResourceType;
import gr.alexc.acodelearn.user.User;
import gr.alexc.acodelearn.user.UserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Course-domain service. Authorization is enforced at the controller boundary
 * via {@code @PreAuthorize} + {@link CourseAccessChecker}; this service
 * focuses on orchestration only.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseQueryHandler courseQueryHandler;
    private final CourseSectionCommandHandler sectionCommandHandler;
    private final ResourceLookup resourceLookup;
    private final UserLookup userLookup;

    public List<CourseSummaryView> getUserCourses(String username) {
        User user = userLookup.findByUsername(username);
        return courseQueryHandler.getCoursesForStudent(user.getId());
    }

    public List<CourseSummaryView> getOwnedCourses(String username) {
        User user = userLookup.findByUsername(username);
        return courseQueryHandler.getOwnedCourses(user.getId());
    }

    @Transactional
    public CourseSummaryView createCourse(CreateCourseCommand command, String username) {
        User user = userLookup.findByUsername(username);
        Course created = sectionCommandHandler.createCourse(command, user.getId());
        return courseQueryHandler.toSummaryView(created);
    }

    @Transactional
    public CourseSummaryView updateCourse(UpdateCourseCommand command) {
        Course updated = sectionCommandHandler.updateCourse(command);
        return courseQueryHandler.toSummaryView(updated);
    }

    public List<ResourceSummaryView> getCourseResources(Long courseId, ResourceType type) {
        List<Resource> resources = type != null
                ? resourceLookup.findByCourseAndType(courseId, type)
                : resourceLookup.findByCourse(courseId);
        return resources.stream().map(resourceLookup::toView).toList();
    }

    public List<CourseSectionView> getSections(Long courseId) {
        return courseQueryHandler.getSections(courseId);
    }

    public CourseSectionView getSection(Long sectionId) {
        return courseQueryHandler.toSectionView(courseQueryHandler.findSectionById(sectionId));
    }

    @Transactional
    public CourseSectionView createSection(CreateCourseSectionCommand command) {
        return courseQueryHandler.toSectionView(sectionCommandHandler.createSection(command));
    }

    @Transactional
    public CourseSectionView updateSection(UpdateCourseSectionCommand command) {
        return courseQueryHandler.toSectionView(sectionCommandHandler.updateSection(command));
    }

    @Transactional
    public List<CourseSectionView> reorderSections(ReorderSectionsCommand command) {
        return sectionCommandHandler.reorderSections(command).stream()
                .map(courseQueryHandler::toSectionView)
                .toList();
    }

    @Transactional
    public void deleteSection(DeleteCourseSectionCommand command) {
        sectionCommandHandler.deleteSection(command);
    }

    @Transactional
    public CourseSectionView addResourcesToSection(AddResourcesToSectionCommand command) {
        return courseQueryHandler.toSectionView(sectionCommandHandler.addResourcesToSection(command));
    }

    @Transactional
    public CourseSectionView removeResourcesFromSection(RemoveResourcesFromSectionCommand command) {
        return courseQueryHandler.toSectionView(sectionCommandHandler.removeResourcesFromSection(command));
    }
}
