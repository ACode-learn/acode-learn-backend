package gr.alexc.acodelearn.resource.internal;

import gr.alexc.acodelearn.resource.Resource;
import gr.alexc.acodelearn.resource.ResourceType;
import org.springframework.data.jpa.domain.Specification;

public final class ResourceSpecifications {

    private ResourceSpecifications() {}

    public static Specification<Resource> byCourseId(Long courseId) {
        return (root, query, cb) -> cb.equal(root.get("courseId"), courseId);
    }

    public static Specification<Resource> byType(ResourceType type) {
        return (root, query, cb) -> cb.equal(root.get("resourceType"), type);
    }
}
