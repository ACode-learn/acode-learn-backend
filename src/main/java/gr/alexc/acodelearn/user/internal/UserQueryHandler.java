package gr.alexc.acodelearn.user.internal;

import gr.alexc.acodelearn.shared.ContentNotFoundException;
import gr.alexc.acodelearn.user.GlobalRole;
import gr.alexc.acodelearn.user.User;
import gr.alexc.acodelearn.user.UserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryHandler implements UserLookup {

    private final UserRepository userRepository;

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ContentNotFoundException("User not found: " + username));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public UserView getView(String username) {
        return toView(findByUsername(username));
    }

    public Page<UserView> getUsersByRole(GlobalRole role, Pageable pageable) {
        return userRepository.findByRolesContaining(role, pageable).map(this::toView);
    }

    UserView toView(User user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRoles()
        );
    }
}
