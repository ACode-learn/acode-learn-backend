package gr.alexc.acodelearn.resource.internal;

import gr.alexc.acodelearn.resource.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {
    List<Resource> findByCourseId(Long courseId);
}
