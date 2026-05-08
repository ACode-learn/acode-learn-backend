package gr.alexc.acodelearn.user;

import gr.alexc.acodelearn.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "\"user\"")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    /**
     * Local snapshot of the user's global Keycloak roles.
     *
     * <p><b>Do not</b> use this for authorization decisions on the current
     * request — use {@code Authentication#getAuthorities()} for that. This
     * field exists for querying/filtering only (e.g. "list all teachers").
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id", nullable = false)
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<GlobalRole> roles = new HashSet<>();
}
