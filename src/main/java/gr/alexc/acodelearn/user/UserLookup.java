package gr.alexc.acodelearn.user;

import java.util.Optional;

public interface UserLookup {

    User findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);
}
