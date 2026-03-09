package gr.alexc.acodelearn.course.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCourseSectionCommand(
        @NotNull Long courseId,
        @NotBlank String name,
        String description,
        Integer order
) {}
