package gr.alexc.acodelearn.course.internal;

import gr.alexc.acodelearn.course.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    boolean existsByCourseIdAndUserIdAndStatus(Long courseId, Long userId, CourseEnrollment.Status status);

    boolean existsByCourseIdAndEmailIgnoreCaseAndStatus(Long courseId, String email, CourseEnrollment.Status status);

    Optional<CourseEnrollment> findByCourseIdAndUserId(Long courseId, Long userId);

    List<CourseEnrollment> findByCourseIdAndStatus(Long courseId, CourseEnrollment.Status status);

    List<CourseEnrollment> findByUserIdAndStatus(Long userId, CourseEnrollment.Status status);

    List<CourseEnrollment> findByEmailIgnoreCaseAndStatus(String email, CourseEnrollment.Status status);

    void deleteByCourseIdAndUserId(Long courseId, Long userId);
}
