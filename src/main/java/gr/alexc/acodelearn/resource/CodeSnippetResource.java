package gr.alexc.acodelearn.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "code_snippet_resource")
@Getter
@Setter
@NoArgsConstructor
public class CodeSnippetResource extends Resource {

    @Column(name = "snippet_title")
    private String snippetTitle;

    @Column(name = "snippet_description", columnDefinition = "TEXT")
    private String snippetDescription;

    @Column(name = "snippet_document_data", columnDefinition = "TEXT")
    private String snippetDocumentData;

    @Column(name = "snippet_language")
    private String snippetLanguage;
}
