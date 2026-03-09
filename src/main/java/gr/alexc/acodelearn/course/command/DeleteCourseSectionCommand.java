package gr.alexc.acodelearn.course.command;

import jakarta.validation.constraints.NotNull;

public record DeleteCourseSectionCommand(@NotNull Long sectionId, @NotNull Long courseId) {}
