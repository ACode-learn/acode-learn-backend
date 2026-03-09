package gr.alexc.acodelearn.user;

public interface UserLookup {
    User findByUsername(String username);
}
