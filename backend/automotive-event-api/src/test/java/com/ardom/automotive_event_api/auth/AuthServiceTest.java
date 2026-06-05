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
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
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

import java.util.Optional;

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
    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

    private final AuthMapper authMapper = new AuthMapper();
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, authMapper, passwordEncoder, jwtService, authenticationManager, refreshTokenService, googleTokenVerifierService);
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

    @Nested
    class GoogleLogin {

        private GoogleIdToken.Payload mockPayload(String email, String googleId,
                                                  String name, String surname) {
            GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
            when(payload.getEmail()).thenReturn(email);
            when(payload.getSubject()).thenReturn(googleId);
            when(payload.get("given_name")).thenReturn(name);
            when(payload.get("family_name")).thenReturn(surname);
            return payload;
        }

        @Test
        @DisplayName("Should create new user and return tokens when Google user does not exist")
        void googleLogin_shouldCreateNewUserAndReturnTokens_whenGoogleUserDoesNotExist() {
            // given
            var payload = mockPayload("new@gmail.com", "g-123", "John", "Doe");
            when(googleTokenVerifierService.verify("valid-token")).thenReturn(payload);
            when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());

            User savedUser = User.builder()
                    .email("new@gmail.com")
                    .name("John")
                    .surname("Doe")
                    .googleId("g-123")
                    .role(Role.USER)
                    .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(savedUser)).thenReturn("access-jwt");
            when(refreshTokenService.createRefreshToken(savedUser)).thenReturn("refresh-jwt");

            // when
            AuthResponse result = authService.googleLogin("valid-token");

            // then
            assertEquals("access-jwt", result.accessToken());
            assertEquals("refresh-jwt", result.refreshToken());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User captured = captor.getValue();
            assertEquals("new@gmail.com", captured.getEmail());
            assertEquals("John", captured.getName());
            assertEquals("Doe", captured.getSurname());
            assertEquals("g-123", captured.getGoogleId());
            assertEquals(Role.USER, captured.getRole());
            assertEquals("", captured.getPassword());
        }

        @Test
        @DisplayName("Should return tokens without saving when Google user already exists")
        void googleLogin_shouldReturnTokensWithoutSaving_whenGoogleUserAlreadyExists() {
            // given
            var payload = mockPayload("existing@gmail.com", "g-123", "John", "Doe");
            when(googleTokenVerifierService.verify("valid-token")).thenReturn(payload);

            User existingUser = User.builder()
                    .email("existing@gmail.com")
                    .googleId("g-123")
                    .role(Role.USER)
                    .build();
            when(userRepository.findByEmail("existing@gmail.com"))
                    .thenReturn(Optional.of(existingUser));
            when(jwtService.generateToken(existingUser)).thenReturn("access-jwt");
            when(refreshTokenService.createRefreshToken(existingUser)).thenReturn("refresh-jwt");

            // when
            AuthResponse result = authService.googleLogin("valid-token");

            // then
            assertEquals("access-jwt", result.accessToken());
            assertEquals("refresh-jwt", result.refreshToken());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when email is already registered locally")
        void googleLogin_shouldThrowUserAlreadyExistsException_whenEmailAlreadyRegisteredLocally() {
            // given
            var payload = mockPayload("local@gmail.com", "g-123", "John", "Doe");
            when(googleTokenVerifierService.verify("valid-token")).thenReturn(payload);

            User localUser = User.builder()
                    .email("local@gmail.com")
                    .googleId(null)
                    .build();
            when(userRepository.findByEmail("local@gmail.com"))
                    .thenReturn(Optional.of(localUser));

            // when & then
            assertThrows(UserAlreadyExistsException.class,
                    () -> authService.googleLogin("valid-token"));

            verify(userRepository, never()).save(any());
            verifyNoInteractions(jwtService);
            verifyNoInteractions(refreshTokenService);
        }

        @Test
        @DisplayName("Should default surname to empty string when family name is null")
        void googleLogin_shouldDefaultSurnameToEmptyString_whenFamilyNameIsNull() {
            // given
            var payload = mockPayload("nosurname@gmail.com", "g-456", "Madonna", null);
            when(googleTokenVerifierService.verify("valid-token")).thenReturn(payload);
            when(userRepository.findByEmail("nosurname@gmail.com")).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any())).thenReturn("access-jwt");
            when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-jwt");

            // when
            authService.googleLogin("valid-token");

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("", captor.getValue().getSurname());
        }

        @Test
        @DisplayName("Should not interact with repository when Google token is invalid")
        void googleLogin_shouldNotInteractWithRepository_whenGoogleTokenIsInvalid() {
            // given
            when(googleTokenVerifierService.verify("bad-token"))
                    .thenThrow(new RuntimeException("Invalid Google token"));

            // when & then
            assertThrows(RuntimeException.class,
                    () -> authService.googleLogin("bad-token"));

            verifyNoInteractions(userRepository);
            verifyNoInteractions(jwtService);
            verifyNoInteractions(refreshTokenService);
        }
    }
}