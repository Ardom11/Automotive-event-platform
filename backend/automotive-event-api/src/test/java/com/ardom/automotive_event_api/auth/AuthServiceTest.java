package com.ardom.automotive_event_api.auth;

import com.ardom.automotive_event_api.auth.dto.request.LoginRequest;
import com.ardom.automotive_event_api.auth.dto.request.RegisterRequest;
import com.ardom.automotive_event_api.auth.dto.response.AuthResponse;
import com.ardom.automotive_event_api.auth.exception.InvalidCredentialsException;
import com.ardom.automotive_event_api.auth.exception.UserAlreadyExistsException;
import com.ardom.automotive_event_api.auth.refreshtoken.RefreshTokenService;
import com.ardom.automotive_event_api.security.JwtService;
import com.ardom.automotive_event_api.user.Role;
import com.ardom.automotive_event_api.user.User;
import com.ardom.automotive_event_api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    private final AuthMapper authMapper = new AuthMapper();
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    //@InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, authMapper, passwordEncoder, jwtService, authenticationManager, refreshTokenService);
    }

    @Nested
    @DisplayName("Method register()")
    class RegisterTests {

        @Test
        @DisplayName("Should save user and return tokens when registering a new user")
        void register_shouldSaveUserAndReturnTokens_whenUserIsNew() {
            // Given
            RegisterRequest request = new RegisterRequest("John", "Doe", "john@gmail.com", "password123");

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn("encoded_password");
            when(jwtService.generateToken(any(User.class))).thenReturn("access_token");
            when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("refresh_token");

            // When
            AuthResponse response = authService.register(request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();

            assertNotNull(response);
            assertEquals("access_token", response.accessToken());
            assertEquals("refresh_token", response.refreshToken());
            assertEquals("encoded_password", capturedUser.getPassword());

            verify(passwordEncoder).encode(request.password());
            verify(jwtService).generateToken(capturedUser);
            verify(refreshTokenService).createRefreshToken(capturedUser);
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException, if email occupied")
        void register_shouldThrowUserAlreadyExistsException_whenEmailIsOccupied() {
            // Given
            RegisterRequest request = new RegisterRequest("John", "Doe", "john@gmail.com", "password123");
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            // When & Then
            assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Method login()")
    class LoginTests {

        @Test
        @DisplayName("Should authorize user then revoke all its refresh tokens and return new refresh and access token")
        void login_shouldRevokeTokensAndReturnNewOnSuccessfulLogin() {
            // Given
            LoginRequest request = new LoginRequest("test@mail.com", "password");
            User user = new User();
            user.setEmail(request.email());
            user.setPassword(request.password());

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtService.generateToken(user)).thenReturn("access_token");
            when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh_token");

            // When
            AuthResponse response = authService.login(request);

            // Then
            assertNotNull(response);
            assertEquals("access_token", response.accessToken());
            assertEquals("refresh_token", response.refreshToken());
            verify(jwtService).generateToken(user);
            verify(refreshTokenService).createRefreshToken(user);
            verify(refreshTokenService).revokeAllUserTokens(user);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException with incorrect password")
        void login_shouldThrowInvalidCredentialsException_whenPasswordIsIncorrect() {
            // Given
            LoginRequest request = new LoginRequest("test@mail.com", "wrong");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When & Then
            assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        }
    }

    @Nested
    @DisplayName("Method refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should update access token by refresh token")
        void refreshToken_shouldReturnNewAccessTokenByRefreshToken_whenRefreshTokenIsValid() {
            // Given
            String oldToken = "old_refresh_token";
            User user = new User();
            user.setEmail("user@mail.com");
            user.setRole(Role.USER);

            var refreshTokenEntity = mock(com.ardom.automotive_event_api.auth.refreshtoken.RefreshToken.class);
            when(refreshTokenEntity.getUser()).thenReturn(user);
            when(refreshTokenService.getValidatedRefreshToken(oldToken)).thenReturn(refreshTokenEntity);
            when(jwtService.generateToken(user)).thenReturn("access_token");
            when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh_token");

            // When
            AuthResponse response = authService.refreshToken(oldToken);

            // Then
            assertNotNull(response);

            assertEquals("access_token", response.accessToken());
            assertEquals("refresh_token", response.refreshToken());

            verify(refreshTokenService).getValidatedRefreshToken(oldToken);
            verify(refreshTokenService).revokeAllUserTokens(user);
            verify(jwtService).generateToken(user);
            verify(refreshTokenService).createRefreshToken(user);
        }
    }
}