package gr.alexc.acodelearn.user.internal;

import gr.alexc.acodelearn.shared.ContentNotFoundException;
import gr.alexc.acodelearn.user.User;
import gr.alexc.acodelearn.user.UserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryHandler implements UserLookup {

    private final UserRepository userRepository;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ContentNotFoundException("User not found: " + username));
    }

    public UserView getView(String username) {
        User user = findByUsername(username);
        return new UserView(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail());
    }
}
