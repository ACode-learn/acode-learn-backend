package gr.alexc.acodelearn.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "repo_resource")
@Getter
@Setter
@NoArgsConstructor
public class RepositoryResource extends Resource {

    @Column(name = "repo_url")
    private String repoUrl;

    @Column(name = "repo_name")
    private String repoName;

    @Column(name = "repo_user_repo")
    private String repoNameRepo;
}
