package gr.alexc.acodelearn.resource;

import java.util.List;

public interface ResourceLookup {
    List<Resource> findByCourse(Long courseId);
    List<Resource> findByCourseAndType(Long courseId, ResourceType type);
    List<Resource> findAllByIds(List<Long> resourceIds);
    Resource findById(Long id);
    ResourceSummaryView toView(Resource resource);
}
