package gr.alexc.acodelearn.user;

import gr.alexc.acodelearn.user.internal.UserQueryHandler;
import gr.alexc.acodelearn.user.internal.UserService;
import gr.alexc.acodelearn.user.internal.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserQueryHandler userQueryHandler;

    @GetMapping
    public ResponseEntity<UserView> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getOrCreateUser(jwt));
    }

    /**
     * Lists local users filtered by global role. Backed by the synced role
     * snapshot, intended for admin/teacher screens (e.g. picking instructors
     * or showing all known students).
     */
    @GetMapping("/by-role")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Page<UserView>> getUsersByRole(
            @RequestParam GlobalRole role,
            Pageable pageable
    ) {
        return ResponseEntity.ok(userQueryHandler.getUsersByRole(role, pageable));
    }
}
