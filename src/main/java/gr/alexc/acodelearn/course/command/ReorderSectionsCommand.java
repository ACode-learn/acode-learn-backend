package gr.alexc.acodelearn.course.command;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ReorderSectionsCommand(@NotNull Long courseId, @NotNull Map<Long, Integer> sectionOrderMap) {}
