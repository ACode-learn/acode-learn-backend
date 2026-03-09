package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_section")
@Getter
@Setter
@NoArgsConstructor
public class CourseSection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "section_order")
    private Integer sectionOrder;

    @ElementCollection
    @CollectionTable(name = "course_section_has_resource", joinColumns = @JoinColumn(name = "course_section_id", nullable = false))
    @Column(name = "resource_id", nullable = false)
    private List<Long> resourceIds = new ArrayList<>();

    public CourseSection(Course course, String name, String description, Integer order) {
        this.course = course;
        this.name = name;
        this.description = description;
        this.sectionOrder = order;
    }
}
