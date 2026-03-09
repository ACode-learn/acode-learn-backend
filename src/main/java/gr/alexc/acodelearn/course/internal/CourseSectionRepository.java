package gr.alexc.acodelearn.course.internal;

import gr.alexc.acodelearn.course.CourseSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {
    List<CourseSection> findByCourseIdOrderBySectionOrderAsc(Long courseId);
}
