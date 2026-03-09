package gr.alexc.acodelearn.resource.command;

import gr.alexc.acodelearn.resource.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateResourceCommand(
        @NotBlank String name,
        @NotNull Long courseId,
        @NotNull ResourceType resourceType,
        Map<String, Object> attributes
) {}
