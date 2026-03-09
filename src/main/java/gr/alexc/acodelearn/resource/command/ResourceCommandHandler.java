package gr.alexc.acodelearn.resource.command;

import gr.alexc.acodelearn.resource.*;
import gr.alexc.acodelearn.resource.internal.ResourceRepository;
import gr.alexc.acodelearn.shared.ContentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ResourceCommandHandler {

    private final ResourceRepository resourceRepository;

    @Transactional
    public Resource createResource(CreateResourceCommand command) {
        Resource resource = buildResource(command.resourceType(), command.attributes());
        resource.setName(command.name());
        resource.setCourseId(command.courseId());
        resource.setResourceType(command.resourceType());
        return resourceRepository.save(resource);
    }

    @Transactional
    public Resource createFileResource(Long courseId, String name, String summary, MultipartFile file) {
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid file path: " + fileName);
        }
        try {
            FileResource resource = new FileResource();
            resource.setName(name);
            resource.setCourseId(courseId);
            resource.setResourceType(ResourceType.FILE);
            resource.setFileName(fileName);
            resource.setFileType(file.getContentType());
            resource.setFileData(file.getBytes());
            resource.setSummary(summary);
            return resourceRepository.save(resource);
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + fileName, e);
        }
    }

    @Transactional
    public Resource updateResource(UpdateResourceCommand command) {
        Resource resource = resourceRepository.findById(command.resourceId())
                .orElseThrow(() -> new ContentNotFoundException("Resource not found: " + command.resourceId()));
        if (command.name() != null) {
            resource.setName(command.name());
        }
        applyAttributes(resource, command.attributes());
        return resourceRepository.save(resource);
    }

    @Transactional
    public void deleteResource(DeleteResourceCommand command) {
        Resource resource = resourceRepository.findById(command.resourceId())
                .orElseThrow(() -> new ContentNotFoundException("Resource not found: " + command.resourceId()));
        resourceRepository.delete(resource);
    }

    private Resource buildResource(ResourceType type, Map<String, Object> attrs) {
        return switch (type) {
            case LINK -> {
                LinkResource r = new LinkResource();
                if (attrs != null) {
                    r.setLink((String) attrs.get("link"));
                    r.setDescription((String) attrs.get("description"));
                }
                yield r;
            }
            case FILE -> new FileResource();
            case REPOSITORY -> {
                RepositoryResource r = new RepositoryResource();
                if (attrs != null) {
                    r.setRepoUrl((String) attrs.get("repoUrl"));
                    r.setRepoName((String) attrs.get("repoName"));
                    r.setRepoNameRepo((String) attrs.get("repoNameRepo"));
                }
                yield r;
            }
            case CODE_SNIPPET -> {
                CodeSnippetResource r = new CodeSnippetResource();
                if (attrs != null) {
                    r.setSnippetTitle((String) attrs.get("snippetTitle"));
                    r.setSnippetDescription((String) attrs.get("snippetDescription"));
                    r.setSnippetDocumentData((String) attrs.get("snippetDocumentData"));
                    r.setSnippetLanguage((String) attrs.get("snippetLanguage"));
                }
                yield r;
            }
            case MARKDOWN -> {
                MarkdownDocumentResource r = new MarkdownDocumentResource();
                if (attrs != null) {
                    r.setDocumentTitle((String) attrs.get("documentTitle"));
                    r.setDescription((String) attrs.get("description"));
                    r.setMarkdownDocumentData((String) attrs.get("markdownDocumentData"));
                }
                yield r;
            }
            case GUIDE -> {
                GuideResource r = new GuideResource();
                if (attrs != null) {
                    r.setGuideTitle((String) attrs.get("guideTitle"));
                    r.setGuideDescription((String) attrs.get("guideDescription"));
                    r.setGuideData((String) attrs.get("guideData"));
                }
                yield r;
            }
        };
    }

    private void applyAttributes(Resource resource, Map<String, Object> attrs) {
        if (attrs == null) return;
        switch (resource) {
            case LinkResource r -> {
                if (attrs.containsKey("link")) r.setLink((String) attrs.get("link"));
                if (attrs.containsKey("description")) r.setDescription((String) attrs.get("description"));
            }
            case RepositoryResource r -> {
                if (attrs.containsKey("repoUrl")) r.setRepoUrl((String) attrs.get("repoUrl"));
                if (attrs.containsKey("repoName")) r.setRepoName((String) attrs.get("repoName"));
                if (attrs.containsKey("repoNameRepo")) r.setRepoNameRepo((String) attrs.get("repoNameRepo"));
            }
            case CodeSnippetResource r -> {
                if (attrs.containsKey("snippetTitle")) r.setSnippetTitle((String) attrs.get("snippetTitle"));
                if (attrs.containsKey("snippetDescription")) r.setSnippetDescription((String) attrs.get("snippetDescription"));
                if (attrs.containsKey("snippetDocumentData")) r.setSnippetDocumentData((String) attrs.get("snippetDocumentData"));
                if (attrs.containsKey("snippetLanguage")) r.setSnippetLanguage((String) attrs.get("snippetLanguage"));
            }
            case MarkdownDocumentResource r -> {
                if (attrs.containsKey("documentTitle")) r.setDocumentTitle((String) attrs.get("documentTitle"));
                if (attrs.containsKey("description")) r.setDescription((String) attrs.get("description"));
                if (attrs.containsKey("markdownDocumentData")) r.setMarkdownDocumentData((String) attrs.get("markdownDocumentData"));
            }
            case GuideResource r -> {
                if (attrs.containsKey("guideTitle")) r.setGuideTitle((String) attrs.get("guideTitle"));
                if (attrs.containsKey("guideDescription")) r.setGuideDescription((String) attrs.get("guideDescription"));
                if (attrs.containsKey("guideData")) r.setGuideData((String) attrs.get("guideData"));
            }
            case FileResource r -> {
                if (attrs.containsKey("summary")) r.setSummary((String) attrs.get("summary"));
            }
            default -> throw new IllegalStateException("Unexpected value: " + resource);
        }
    }
}
