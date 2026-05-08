package gr.alexc.acodelearn.course.command;

import gr.alexc.acodelearn.course.content.SectionContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCourseSectionCommand(
        @NotNull Long courseId,
        @NotBlank String name,
        String description,
        Integer order,
        @Valid SectionContent content
) {}
