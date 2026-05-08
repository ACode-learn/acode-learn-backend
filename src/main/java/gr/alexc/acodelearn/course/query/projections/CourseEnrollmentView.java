package gr.alexc.acodelearn.course.query.projections;

import gr.alexc.acodelearn.course.CourseEnrollment;

import java.time.Instant;

public record CourseEnrollmentView(
        Long id,
        Long courseId,
        Long userId,
        String email,
        CourseEnrollment.Status status,
        Instant activatedAt
) {
    public static CourseEnrollmentView of(CourseEnrollment e) {
        return new CourseEnrollmentView(
                e.getId(),
                e.getCourseId(),
                e.getUserId(),
                e.getEmail(),
                e.getStatus(),
                e.getActivatedAt()
        );
    }
}
