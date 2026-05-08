package gr.alexc.acodelearn.course.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SectionBlock(
        @NotBlank String id,
        @NotNull SectionBlockType type,
        Map<String, Object> data
) {
}
