package gr.alexc.acodelearn.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "markdown_document_resource")
@Getter
@Setter
@NoArgsConstructor
public class MarkdownDocumentResource extends Resource {

    @Column(name = "md_title")
    private String documentTitle;

    @Column(name = "md_description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "md_document_data", columnDefinition = "TEXT")
    private String markdownDocumentData;
}
