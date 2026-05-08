package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A student's enrollment in a course.
 *
 * <p>An enrollment is in one of two states:
 * <ul>
 *     <li>{@link Status#ACTIVE}: the user is known locally and {@code userId}
 *     is set; this enrollment grants access to the course.</li>
 *     <li>{@link Status#PENDING}: the instructor enrolled a user by email
 *     before that user existed locally. Only {@code email} is set. When the
 *     user logs in for the first time the enrollment is upgraded to
 *     {@code ACTIVE} and {@code userId} is populated.</li>
 * </ul>
 */
@Entity
@Table(name = "course_enrollment")
@Getter
@Setter
@NoArgsConstructor
public class CourseEnrollment extends BaseEntity {

    public enum Status {
        PENDING,
        ACTIVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /** Set when the enrollment is {@link Status#ACTIVE}. */
    @Column(name = "user_id")
    private Long userId;

    /** Set when the enrollment was created from an email (may stay set after activation). */
    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "activated_at")
    private Instant activatedAt;

    public static CourseEnrollment active(Long courseId, Long userId, String email) {
        CourseEnrollment e = new CourseEnrollment();
        e.courseId = courseId;
        e.userId = userId;
        e.email = email;
        e.status = Status.ACTIVE;
        e.activatedAt = Instant.now();
        return e;
    }

    public static CourseEnrollment pending(Long courseId, String email) {
        CourseEnrollment e = new CourseEnrollment();
        e.courseId = courseId;
        e.email = email;
        e.status = Status.PENDING;
        return e;
    }

    public void activate(Long userId) {
        this.userId = userId;
        this.status = Status.ACTIVE;
        this.activatedAt = Instant.now();
    }
}
