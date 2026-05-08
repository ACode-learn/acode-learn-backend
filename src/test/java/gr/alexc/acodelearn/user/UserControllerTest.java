package gr.alexc.acodelearn.user;

import gr.alexc.acodelearn.user.internal.UserQueryHandler;
import gr.alexc.acodelearn.user.internal.UserService;
import gr.alexc.acodelearn.user.internal.UserView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserQueryHandler userQueryHandler;

    @Test
    void getCurrentUser_WhenAuthenticated_ShouldReturnUser() throws Exception {
        // Arrange
        UserView userView = new UserView(1L, "testuser", "Test", "User", "test@example.com", java.util.Set.of(GlobalRole.STUDENT));
        when(userService.getOrCreateUser(any(Jwt.class))).thenReturn(userView);

        // Act & Assert
        mockMvc.perform(get("/user")
                        .with(jwt().jwt(j -> j.subject("test-sub"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getCurrentUser_WhenUnauthenticated_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUsersByRole_asTeacher_returnsPage() throws Exception {
        UserView v = new UserView(1L, "alice", "A", "B", "a@b.com", java.util.Set.of(GlobalRole.STUDENT));
        when(userQueryHandler.getUsersByRole(eq(GlobalRole.STUDENT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));

        mockMvc.perform(get("/user/by-role").param("role", "STUDENT")
                        .with(jwt().jwt(j -> j.subject("s").claim("preferred_username", "u"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getUsersByRole_asAdmin_returnsPage() throws Exception {
        when(userQueryHandler.getUsersByRole(eq(GlobalRole.TEACHER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user/by-role").param("role", "TEACHER")
                        .with(jwt().jwt(j -> j.subject("s").claim("preferred_username", "admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void getUsersByRole_asStudent_isForbidden() throws Exception {
        mockMvc.perform(get("/user/by-role").param("role", "STUDENT")
                        .with(jwt().jwt(j -> j.subject("s").claim("preferred_username", "u"))
                                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsersByRole_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/user/by-role").param("role", "STUDENT"))
                .andExpect(status().isUnauthorized());
    }
}
