package gr.alexc.acodelearn.course.command;

import gr.alexc.acodelearn.course.Course;
import gr.alexc.acodelearn.course.CourseSection;
import gr.alexc.acodelearn.course.internal.CourseRepository;
import gr.alexc.acodelearn.course.internal.CourseSectionRepository;
import gr.alexc.acodelearn.course.query.CourseQueryHandler;
import gr.alexc.acodelearn.resource.ResourceLookup;
import gr.alexc.acodelearn.shared.ContentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseSectionCommandHandler {

    private final CourseRepository courseRepository;
    private final ResourceLookup resourceLookup;

    @Transactional
    public CourseSection createSection(CreateCourseSectionCommand command) {
        Course course = findCourse(command.courseId());
        CourseSection section = course.addSection(command.name(), command.description(), command.order());
        courseRepository.save(course);
        return section;
    }

    @Transactional
    public CourseSection updateSection(UpdateCourseSectionCommand command) {
        Course course = findCourse(command.courseId());
        CourseSection section = course.updateSection(command.sectionId(), command.name(), command.description(), command.order());
        courseRepository.save(course);
        return section;
    }

    @Transactional
    public void deleteSection(DeleteCourseSectionCommand command) {
        Course course = findCourse(command.courseId());
        course.removeSection(command.sectionId());
        courseRepository.save(course);
    }

    @Transactional
    public List<CourseSection> reorderSections(ReorderSectionsCommand command) {
        Course course = findCourse(command.courseId());
        course.reorderSections(command.sectionOrderMap());
        courseRepository.save(course);
        return course.getCourseSections();
    }

    @Transactional
    public CourseSection addResourcesToSection(AddResourcesToSectionCommand command) {
        Course course = findCourse(command.courseId());
        List<Long> resourceIds = command.resourceIds();
        int foundCount = resourceLookup.findAllByIds(resourceIds).size();
        if (foundCount != resourceIds.size()) {
            throw new ContentNotFoundException("One or more resources not found");
        }
        course.addResourcesToSection(command.sectionId(), resourceIds);
        courseRepository.save(course);
        return findSection(course, command.sectionId());
    }

    @Transactional
    public CourseSection removeResourcesFromSection(RemoveResourcesFromSectionCommand command) {
        Course course = findCourse(command.courseId());
        course.removeResourcesFromSection(command.sectionId(), command.resourceIds());
        courseRepository.save(course);
        return findSection(course, command.sectionId());
    }

    @Transactional
    public Course updateCourse(UpdateCourseCommand command) {
        Course course = findCourse(command.courseId());
        course.updateDetails(command.title(), command.description(), command.semester());
        return courseRepository.save(course);
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException("Course not found: " + id));
    }

    private CourseSection findSection(Course course, Long sectionId) {
        return course.getCourseSections().stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new ContentNotFoundException("Section not found: " + sectionId));
    }
}
