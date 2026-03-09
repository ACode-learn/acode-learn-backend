package gr.alexc.acodelearn.course.query;

import gr.alexc.acodelearn.course.Course;
import gr.alexc.acodelearn.course.CourseSection;
import gr.alexc.acodelearn.course.internal.CourseRepository;
import gr.alexc.acodelearn.course.internal.CourseSectionRepository;
import gr.alexc.acodelearn.course.query.projections.CourseSectionView;
import gr.alexc.acodelearn.course.query.projections.CourseSummaryView;
import gr.alexc.acodelearn.shared.ContentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryHandler {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;

    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException("Course not found: " + id));
    }

    public CourseSummaryView getSummary(Long id) {
        Course c = findById(id);
        return new CourseSummaryView(c.getId(), c.getTitle(), c.getDescription(), c.getSemester());
    }

    public List<CourseSummaryView> getCoursesForStudent(Long userId) {
        return courseRepository.findByEnrolledStudentIdsContaining(userId).stream()
                .map(this::toSummaryView)
                .toList();
    }

    public List<CourseSummaryView> getOwnedCourses(Long userId) {
        return courseRepository.findByInstructorIdsContaining(userId).stream()
                .map(this::toSummaryView)
                .toList();
    }

    public CourseSummaryView toSummaryView(Course c) {
        return new CourseSummaryView(c.getId(), c.getTitle(), c.getDescription(), c.getSemester());
    }

    public List<CourseSectionView> getSections(Long courseId) {
        return sectionRepository.findByCourseIdOrderBySectionOrderAsc(courseId).stream()
                .map(this::toSectionView)
                .toList();
    }

    public CourseSection findSectionById(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ContentNotFoundException("Section not found: " + sectionId));
    }

    public CourseSectionView toSectionView(CourseSection cs) {
        return new CourseSectionView(
                cs.getId(),
                cs.getCourse().getId(),
                cs.getName(),
                cs.getDescription(),
                cs.getSectionOrder(),
                cs.getCreatedAt()
        );
    }
}
