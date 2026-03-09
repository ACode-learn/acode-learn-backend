package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.course.command.*;
import gr.alexc.acodelearn.course.query.CourseQueryHandler;
import gr.alexc.acodelearn.course.query.projections.CourseSectionView;
import gr.alexc.acodelearn.course.query.projections.CourseSummaryView;
import gr.alexc.acodelearn.resource.Resource;
import gr.alexc.acodelearn.resource.ResourceLookup;
import gr.alexc.acodelearn.resource.ResourceSummaryView;
import gr.alexc.acodelearn.resource.ResourceType;
import gr.alexc.acodelearn.shared.UserNotAllowedException;
import gr.alexc.acodelearn.user.User;
import gr.alexc.acodelearn.user.UserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
    public CourseSummaryView updateCourse(Long courseId, UpdateCourseCommand command, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(courseId);
        requireInstructor(user, course);
        Course updated = sectionCommandHandler.updateCourse(command);
        return new CourseSummaryView(updated.getId(), updated.getTitle(), updated.getDescription(), updated.getSemester());
    }

    public List<ResourceSummaryView> getCourseResources(Long courseId, ResourceType type, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(courseId);
        requireEnrolledOrInstructor(user, course);
        List<Resource> resources = type != null
                ? resourceLookup.findByCourseAndType(courseId, type)
                : resourceLookup.findByCourse(courseId);
        return resources.stream().map(resourceLookup::toView).toList();
    }

    public List<CourseSectionView> getSections(Long courseId, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(courseId);
        requireEnrolledOrInstructor(user, course);
        return courseQueryHandler.getSections(courseId);
    }

    public CourseSectionView getSection(Long courseId, Long sectionId, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(courseId);
        requireEnrolledOrInstructor(user, course);
        CourseSection section = courseQueryHandler.findSectionById(sectionId);
        return courseQueryHandler.toSectionView(section);
    }

    @Transactional
    public CourseSectionView createSection(CreateCourseSectionCommand command, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(command.courseId());
        requireInstructor(user, course);
        CourseSection section = sectionCommandHandler.createSection(command);
        return courseQueryHandler.toSectionView(section);
    }

    @Transactional
    public CourseSectionView updateSection(UpdateCourseSectionCommand command, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(command.courseId());
        requireInstructor(user, course);
        CourseSection updated = sectionCommandHandler.updateSection(command);
        return courseQueryHandler.toSectionView(updated);
    }

    @Transactional
    public List<CourseSectionView> reorderSections(ReorderSectionsCommand command, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(command.courseId());
        requireInstructor(user, course);
        List<CourseSection> updated = sectionCommandHandler.reorderSections(command);
        return updated.stream().map(courseQueryHandler::toSectionView).toList();
    }

    @Transactional
    public void deleteSection(DeleteCourseSectionCommand command, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(command.courseId());
        requireInstructor(user, course);
        sectionCommandHandler.deleteSection(command);
    }

    @Transactional
    public CourseSectionView addResourcesToSection(AddResourcesToSectionCommand command, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(command.courseId());
        requireInstructor(user, course);
        CourseSection updated = sectionCommandHandler.addResourcesToSection(command);
        return courseQueryHandler.toSectionView(updated);
    }

    @Transactional
    public CourseSectionView removeResourcesFromSection(RemoveResourcesFromSectionCommand command, String username) {
        User user = userLookup.findByUsername(username);
        Course course = courseQueryHandler.findById(command.courseId());
        requireInstructor(user, course);
        CourseSection updated = sectionCommandHandler.removeResourcesFromSection(command);
        return courseQueryHandler.toSectionView(updated);
    }

    private void requireInstructor(User user, Course course) {
        if (!course.hasInstructor(user.getId())) {
            throw new UserNotAllowedException("User is not an instructor of this course");
        }
    }

    private void requireEnrolledOrInstructor(User user, Course course) {
        if (!course.hasStudent(user.getId()) && !course.hasInstructor(user.getId())) {
            throw new UserNotAllowedException("User is not enrolled or an instructor of this course");
        }
    }
}
