package gr.alexc.acodelearn.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "guide_resource")
@Getter
@Setter
@NoArgsConstructor
public class GuideResource extends Resource {

    @Column(name = "guide_title")
    private String guideTitle;

    @Column(name = "guide_description", columnDefinition = "TEXT")
    private String guideDescription;

    @Column(name = "guide_data", columnDefinition = "TEXT")
    private String guideData;
}
