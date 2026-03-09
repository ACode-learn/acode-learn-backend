package gr.alexc.acodelearn.resource.command;

import jakarta.validation.constraints.NotNull;

public record DeleteResourceCommand(@NotNull Long resourceId, @NotNull Long courseId) {}
