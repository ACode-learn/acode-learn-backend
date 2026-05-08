package gr.alexc.acodelearn.course.command;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseCommand(
        @NotBlank String title,
        String description,
        Integer semester
) {}
