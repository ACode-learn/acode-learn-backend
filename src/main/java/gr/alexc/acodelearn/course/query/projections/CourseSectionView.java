package gr.alexc.acodelearn.course.query.projections;

import gr.alexc.acodelearn.course.content.SectionContent;

import java.time.Instant;

public record CourseSectionView(
        Long id,
        Long courseId,
        String name,
        String description,
        Integer order,
        SectionContent content,
        Instant createdAt
) {}
