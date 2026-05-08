package gr.alexc.acodelearn.course;

import gr.alexc.acodelearn.course.content.SectionContent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * The single owner of the course (typically the creator). The owner is
     * always also an instructor. ADMIN can override owner-only operations.
     */
    @Column(name = "owner_id")
    private Long ownerId;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Local ids of users who can teach/edit this course (includes the owner).
     * Membership in this set requires a global {@code TEACHER} role; that
     * invariant is enforced by the application layer when adding instructors.
     */
    @ElementCollection
    @CollectionTable(name = "course_instructor", joinColumns = @JoinColumn(name = "course_id", nullable = false))
    @Column(name = "user_id", nullable = false)
    private Set<Long> instructorIds = new HashSet<>();

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "course",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sectionOrder ASC")
    private List<CourseSection> courseSections = new ArrayList<>();

    public CourseSection addSection(String name, String description, Integer order, SectionContent content) {
        CourseSection section = new CourseSection(this, name, description, order);
        if (content != null) {
            section.setContent(content);
        }
        this.courseSections.add(section);
        registerEvent(new CourseSectionCreatedEvent(this.id));
        return section;
    }

    public boolean isOwner(Long userId) {
        return userId != null && userId.equals(ownerId);
    }

    public boolean hasInstructor(Long userId) {
        return userId != null && instructorIds.contains(userId);
    }

    /**
     * Records {@code userId} as the owner of this course. The owner is also
     * always an instructor.
     */
    public void assignOwner(Long userId) {
        this.ownerId = userId;
        if (userId != null) {
            this.instructorIds.add(userId);
        }
    }

    public void addInstructor(Long userId) {
        if (userId != null) {
            this.instructorIds.add(userId);
        }
    }

    public void removeInstructor(Long userId) {
        if (userId == null) return;
        if (userId.equals(ownerId)) {
            throw new IllegalStateException("The course owner cannot be removed from instructors. Transfer ownership first.");
        }
        this.instructorIds.remove(userId);
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

    public CourseSection updateSection(Long sectionId, String name, String description, Integer order, SectionContent content) {
        CourseSection section = findSection(sectionId);
        if (name != null) section.setName(name);
        if (description != null) section.setDescription(description);
        if (order != null) section.setSectionOrder(order);
        if (content != null) section.setContent(content);
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
