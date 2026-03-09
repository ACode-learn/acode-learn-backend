package gr.alexc.acodelearn.course.command;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddResourcesToSectionCommand(
        @NotNull Long courseId,
        @NotNull Long sectionId,
        @NotNull List<Long> resourceIds
) {}
