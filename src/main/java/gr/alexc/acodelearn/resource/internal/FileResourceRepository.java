package gr.alexc.acodelearn.resource.internal;

import gr.alexc.acodelearn.resource.FileResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileResourceRepository extends JpaRepository<FileResource, Long> {
    Optional<FileResource> findById(Long id);
}
