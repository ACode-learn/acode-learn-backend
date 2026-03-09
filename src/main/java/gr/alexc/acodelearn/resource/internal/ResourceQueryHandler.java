package gr.alexc.acodelearn.resource.internal;

import gr.alexc.acodelearn.resource.FileResource;
import gr.alexc.acodelearn.resource.Resource;
import gr.alexc.acodelearn.resource.ResourceLookup;
import gr.alexc.acodelearn.resource.ResourceSummaryView;
import gr.alexc.acodelearn.resource.ResourceType;
import gr.alexc.acodelearn.shared.ContentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceQueryHandler implements ResourceLookup {

    private final ResourceRepository resourceRepository;
    private final FileResourceRepository fileResourceRepository;

    public Resource findById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException("Resource not found: " + id));
    }

    public FileResource findFileById(Long id) {
        return fileResourceRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException("File resource not found: " + id));
    }

    public List<Resource> findByCourse(Long courseId) {
        return resourceRepository.findByCourseId(courseId);
    }

    public List<Resource> findByCourseAndType(Long courseId, ResourceType type) {
        Specification<Resource> spec = ResourceSpecifications.byCourseId(courseId)
                .and(ResourceSpecifications.byType(type));
        return resourceRepository.findAll(spec);
    }

    public List<Resource> findAllByIds(List<Long> ids) {
        return resourceRepository.findAllById(ids);
    }

    public ResourceSummaryView toView(Resource resource) {
        return new ResourceSummaryView(
                resource.getId(),
                resource.getName(),
                resource.getResourceType(),
                resource.getCourseId(),
                resource.getCreatedAt()
        );
    }
}
