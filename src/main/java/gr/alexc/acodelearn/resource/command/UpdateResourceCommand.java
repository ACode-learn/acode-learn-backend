package gr.alexc.acodelearn.resource.command;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateResourceCommand(
        @NotNull Long resourceId,
        @NotNull Long courseId,
        String name,
        Map<String, Object> attributes
) {}
