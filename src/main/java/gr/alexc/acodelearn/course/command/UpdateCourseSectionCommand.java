package gr.alexc.acodelearn.course.command;

import gr.alexc.acodelearn.course.content.SectionContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateCourseSectionCommand(
        @NotNull Long sectionId,
        @NotNull Long courseId,
        String name,
        String description,
        Integer order,
        @Valid SectionContent content
) {}
