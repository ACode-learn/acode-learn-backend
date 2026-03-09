package gr.alexc.acodelearn.course.internal;

import gr.alexc.acodelearn.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByEnrolledStudentIdsContaining(Long studentId);
    List<Course> findByInstructorIdsContaining(Long instructorId);
}
