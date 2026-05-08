package gr.alexc.acodelearn.user.internal;

import gr.alexc.acodelearn.user.GlobalRole;

import java.util.Set;

public record UserView(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        Set<GlobalRole> roles
) {}
