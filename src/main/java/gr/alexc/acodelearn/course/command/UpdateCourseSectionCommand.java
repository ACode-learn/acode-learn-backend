package gr.alexc.acodelearn.course.command;

import jakarta.validation.constraints.NotNull;

public record UpdateCourseSectionCommand(
        @NotNull Long sectionId,
        @NotNull Long courseId,
        String name,
        String description,
        Integer order
) {}
