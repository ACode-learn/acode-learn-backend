package gr.alexc.acodelearn.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "link_resource")
@Getter
@Setter
@NoArgsConstructor
public class LinkResource extends Resource {

    @Column(name = "link")
    private String link;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
