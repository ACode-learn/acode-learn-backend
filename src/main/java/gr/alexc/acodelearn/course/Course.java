package gr.alexc.acodelearn.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
public class Course extends AbstractAggregateRoot<Course> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "section_name")
    private String sectionName = "Section";

    @Column(name = "semester")
    private Integer semester;

    @Version
    @Column(name = "version")
    private Long version;

    @ElementCollection
    @CollectionTable(name = "course_has_user", joinColumns = @JoinColumn(name = "course_id", nullable = false))
    @Column(name = "user_id", nullable = false)
    private List<Long> enrolledStudentIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_has_course", joinColumns = @JoinColumn(name = "course_id", nullable = false))
    @Column(name = "user_id", nullable = false)
    private List<Long> instructorIds = new ArrayList<>();

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "course",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sectionOrder ASC")
    private List<CourseSection> courseSections = new ArrayList<>();

    public CourseSection addSection(String name, String description, Integer order) {
        CourseSection section = new CourseSection(this, name, description, order);
        this.courseSections.add(section);
        registerEvent(new CourseSectionCreatedEvent(this.id));
        return section;
    }

    public boolean hasInstructor(Long userId) {
        return instructorIds.contains(userId);
    }

    public boolean hasStudent(Long userId) {
        return enrolledStudentIds.contains(userId);
    }

    public void updateDetails(String title, String description, Integer semester) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (semester != null) this.semester = semester;
        registerEvent(new CourseUpdatedEvent(this.id));
    }

    public void reorderSections(Map<Long, Integer> orderMap) {
        this.courseSections.forEach(section -> {
            if (orderMap.containsKey(section.getId())) {
                section.setSectionOrder(orderMap.get(section.getId()));
            }
        });
    }

    public CourseSection updateSection(Long sectionId, String name, String description, Integer order) {
        CourseSection section = findSection(sectionId);
        if (name != null) section.setName(name);
        if (description != null) section.setDescription(description);
        if (order != null) section.setSectionOrder(order);
        return section;
    }

    public void addResourcesToSection(Long sectionId, List<Long> resourceIds) {
        CourseSection section = findSection(sectionId);
        section.getResourceIds().addAll(resourceIds);
    }

    public void removeResourcesFromSection(Long sectionId, List<Long> resourceIds) {
        CourseSection section = findSection(sectionId);
        section.getResourceIds().removeAll(resourceIds);
    }

    public void removeSection(Long sectionId) {
        CourseSection section = findSection(sectionId);
        this.courseSections.remove(section);
    }

    private CourseSection findSection(Long sectionId) {
        return this.courseSections.stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new gr.alexc.acodelearn.shared.ContentNotFoundException("Section not found: " + sectionId));
    }
}
