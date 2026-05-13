package gr.alexc.acodelearn.user.internal;

import gr.alexc.acodelearn.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private Environment environment;
    @InjectMocks
    private UserService userService;

    private Jwt jwt;
    private final String SUB = "test-sub";
    private final String USERNAME = "testuser";
    private final String EMAIL = "test@example.com";
    private final String FIRST_NAME = "Test";
    private final String LAST_NAME = "User";

    @BeforeEach
    void setUp() {
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(SUB);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(USERNAME);
        when(jwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(jwt.getClaimAsString("given_name")).thenReturn(FIRST_NAME);
        when(jwt.getClaimAsString("family_name")).thenReturn(LAST_NAME);
    }

    @Test
    void getOrCreateUser_WhenUserDoesNotExist_ShouldCreateUser() {
        // Arrange
        when(userRepository.findByExternalId(SUB)).thenReturn(Optional.empty());
        when(environment.acceptsProfiles(Profiles.of("dev"))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Act
        UserView result = userService.getOrCreateUser(jwt);

        // Assert
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo(USERNAME);
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.firstName()).isEqualTo(FIRST_NAME);
        assertThat(result.lastName()).isEqualTo(LAST_NAME);

        verify(userRepository).findByExternalId(SUB);
        verify(userRepository, never()).findByEmailIgnoreCase(any());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getOrCreateUser_WhenUserExists_ShouldSyncUser() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setExternalId(SUB);
        existingUser.setUsername("old-username");

        when(userRepository.findByExternalId(SUB)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // Act
        UserView result = userService.getOrCreateUser(jwt);

        // Assert
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo(USERNAME);
        assertThat(existingUser.getUsername()).isEqualTo(USERNAME);

        verify(userRepository).findByExternalId(SUB);
        verify(userRepository, never()).findByEmailIgnoreCase(any());
        verify(userRepository).save(existingUser);
    }

    @Test
    void getOrCreateUser_WhenDevProfileAndExternalIdNotFoundButEmailExists_ShouldRelinkUser() {
        // Arrange
        String oldExternalId = "old-keycloak-sub";

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setExternalId(oldExternalId);
        existingUser.setUsername("old-username");
        existingUser.setEmail(EMAIL);

        when(userRepository.findByExternalId(SUB)).thenReturn(Optional.empty());
        when(environment.acceptsProfiles(Profiles.of("dev"))).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // Act
        UserView result = userService.getOrCreateUser(jwt);

        // Assert
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo(USERNAME);
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(existingUser.getExternalId()).isEqualTo(SUB);

        verify(userRepository).findByExternalId(SUB);
        verify(userRepository).findByEmailIgnoreCase(EMAIL);
        verify(userRepository).save(existingUser);
    }

    @Test
    void getOrCreateUser_WhenNotDevProfileAndExternalIdNotFound_ShouldNotRelinkByEmail() {
        // Arrange
        when(userRepository.findByExternalId(SUB)).thenReturn(Optional.empty());
        when(environment.acceptsProfiles(Profiles.of("dev"))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // Act
        UserView result = userService.getOrCreateUser(jwt);

        // Assert
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.email()).isEqualTo(EMAIL);

        verify(userRepository).findByExternalId(SUB);
        verify(userRepository, never()).findByEmailIgnoreCase(any());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getOrCreateUser_WhenRaceConditionOccurs_ShouldRetryAndSucceed() {
        // Arrange
        when(userRepository.findByExternalId(SUB))
                .thenReturn(Optional.empty()) // First call in getOrCreateUser
                .thenReturn(Optional.of(new User())); // Second call in catch block

        when(environment.acceptsProfiles(Profiles.of("dev"))).thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key")) // First save attempt
                .thenAnswer(invocation -> invocation.getArgument(0)); // Second save attempt (sync)

        // Act
        UserView result = userService.getOrCreateUser(jwt);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository, times(2)).findByExternalId(SUB);
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void getOrCreateUser_WhenRaceConditionOccursAndSecondLookupFails_ShouldThrow() {
        // Arrange
        when(userRepository.findByExternalId(SUB)).thenReturn(Optional.empty());
        when(environment.acceptsProfiles(Profiles.of("dev"))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // Act & Assert
        assertThatThrownBy(() -> userService.getOrCreateUser(jwt))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(userRepository, times(2)).findByExternalId(SUB);
    }
}
