package gr.alexc.acodelearn.user.internal;

import gr.alexc.acodelearn.user.GlobalRole;
import gr.alexc.acodelearn.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByExternalId(String externalId);

    Optional<User> findByEmailIgnoreCase(String email);

    Page<User> findByRolesContaining(GlobalRole role, Pageable pageable);
}
