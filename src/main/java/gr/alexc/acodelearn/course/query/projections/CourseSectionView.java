package gr.alexc.acodelearn.course.query.projections;

import java.time.Instant;

public record CourseSectionView(Long id, Long courseId, String name, String description, Integer order, Instant createdAt) {}
