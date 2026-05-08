package gr.alexc.acodelearn.course.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public record SectionContent(
        @NotNull Integer version,
        SectionLayout layout,
        @Valid List<SectionBlock> blocks
) {
    public SectionContent {
        if (version == null) {
            version = 1;
        }
        if (layout == null) {
            layout = SectionLayout.LESSON;
        }
        if (blocks == null) {
            blocks = new ArrayList<>();
        }
    }

    public static SectionContent empty() {
        return new SectionContent(1, SectionLayout.LESSON, List.of());
    }
}
