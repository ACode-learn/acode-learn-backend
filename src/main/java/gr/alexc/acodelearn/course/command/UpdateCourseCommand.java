package gr.alexc.acodelearn.course.command;

import jakarta.validation.constraints.NotNull;

public record UpdateCourseCommand(
        @NotNull Long courseId,
        String title,
        String description,
        Integer semester
) {}
