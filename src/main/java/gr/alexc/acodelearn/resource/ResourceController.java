package gr.alexc.acodelearn.resource;

import gr.alexc.acodelearn.resource.command.CreateResourceCommand;
import gr.alexc.acodelearn.resource.command.DeleteResourceCommand;
import gr.alexc.acodelearn.resource.command.UpdateResourceCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/course/{courseId}/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<ResourceSummaryView>> getCourseResources(
            @PathVariable Long courseId,
            @RequestParam(required = false) ResourceType type
    ) {
        return ResponseEntity.ok(resourceService.getCourseResources(courseId, type));
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<Resource> getResource(
            @PathVariable Long courseId,
            @PathVariable Long resourceId
    ) {
        return ResponseEntity.ok(resourceService.getResource(resourceId));
    }

    @GetMapping("/{resourceId}/file")
    public ResponseEntity<byte[]> getFile(
            @PathVariable Long courseId,
            @PathVariable Long resourceId
    ) {
        FileResource fileResource = resourceService.getFile(resourceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileResource.getFileName());
        return ResponseEntity.ok().headers(headers).body(fileResource.getFileData());
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Resource> createResource(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateResourceCommand command
    ) {
        return ResponseEntity.ok(resourceService.createResource(command));
    }

    @PostMapping("/file")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Resource> createFileResource(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "summary", required = false) String summary
    ) {
        return ResponseEntity.ok(resourceService.createFileResource(courseId, name, summary, file));
    }

    @PutMapping("/{resourceId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Resource> updateResource(
            @PathVariable Long courseId,
            @PathVariable Long resourceId,
            @Valid @RequestBody UpdateResourceCommand command
    ) {
        return ResponseEntity.ok(resourceService.updateResource(command));
    }

    @DeleteMapping("/{resourceId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteResource(
            @PathVariable Long courseId,
            @PathVariable Long resourceId
    ) {
        resourceService.deleteResource(new DeleteResourceCommand(resourceId, courseId));
        return ResponseEntity.noContent().build();
    }
}
