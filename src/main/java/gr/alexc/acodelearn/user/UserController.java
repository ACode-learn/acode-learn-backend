package gr.alexc.acodelearn.user;

import gr.alexc.acodelearn.user.internal.UserQueryHandler;
import gr.alexc.acodelearn.user.internal.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryHandler userQueryHandler;

    @GetMapping
    public ResponseEntity<UserView> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userQueryHandler.getView(jwt.getSubject()));
    }
}
