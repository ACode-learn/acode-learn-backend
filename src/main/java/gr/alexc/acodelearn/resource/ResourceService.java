package gr.alexc.acodelearn.resource;

import gr.alexc.acodelearn.resource.command.CreateResourceCommand;
import gr.alexc.acodelearn.resource.command.DeleteResourceCommand;
import gr.alexc.acodelearn.resource.command.ResourceCommandHandler;
import gr.alexc.acodelearn.resource.command.UpdateResourceCommand;
import gr.alexc.acodelearn.resource.internal.ResourceQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceCommandHandler commandHandler;
    private final ResourceQueryHandler queryHandler;

    public List<ResourceSummaryView> getCourseResources(Long courseId, ResourceType type) {
        List<Resource> resources = type != null
                ? queryHandler.findByCourseAndType(courseId, type)
                : queryHandler.findByCourse(courseId);
        return resources.stream().map(queryHandler::toView).toList();
    }

    public Resource getResource(Long resourceId) {
        return queryHandler.findById(resourceId);
    }

    public FileResource getFile(Long resourceId) {
        return queryHandler.findFileById(resourceId);
    }

    @Transactional
    public Resource createResource(CreateResourceCommand command) {
        return commandHandler.createResource(command);
    }

    @Transactional
    public Resource createFileResource(Long courseId, String name, String summary, MultipartFile file) {
        return commandHandler.createFileResource(courseId, name, summary, file);
    }

    @Transactional
    public Resource updateResource(UpdateResourceCommand command) {
        return commandHandler.updateResource(command);
    }

    @Transactional
    public void deleteResource(DeleteResourceCommand command) {
        commandHandler.deleteResource(command);
    }
}
