package com.ardom.automotive_event_api.auth.refreshtoken;

import com.ardom.automotive_event_api.auth.exception.InvalidRefreshTokenException;
import com.ardom.automotive_event_api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should create and save Refresh Token")
    void createRefreshToken_shouldCreateAndSaveRefreshToken() {
        // When
        String token = refreshTokenService.createRefreshToken(testUser);

        // Then
        assertFalse(token.isBlank());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertEquals(testUser, savedToken.getUser());
        assertEquals(token, savedToken.getToken());
        assertFalse(savedToken.isRevoked());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should return token, if it exist, not revoked and not expired")
    void getValidatedRefreshToken_shouldReturnExistingValidatedRefreshTokenObject_whenPassedToken() {
        // Given
        String rawToken = "valid-uuid-string";
        RefreshToken expectedToken = RefreshToken.builder()
                .token(rawToken)
                .revoked(false)
                .build();

        // Using any(), because LocalDateTime.now() in service will create new object
        when(refreshTokenRepository.findByTokenAndRevokedFalseAndExpiresAtAfter(eq(rawToken), any(LocalDateTime.class)))
                .thenReturn(Optional.of(expectedToken));

        // When
        RefreshToken result = refreshTokenService.getValidatedRefreshToken(rawToken);

        // Then
        assertNotNull(result);
        assertEquals(rawToken, result.getToken());
        assertFalse(result.isRevoked());
    }

    @Test
    @DisplayName("Should throw InvalidRefreshTokenException, if token is not found or expired or revoked")
    void getValidatedRefreshToken_shouldThrowInvalidRefreshTokenException_whenTokenIsNotFoundOrExpiredOrRevoked() {
        // Given
        String rawToken = "invalid-token";
        when(refreshTokenRepository.findByTokenAndRevokedFalseAndExpiresAtAfter(eq(rawToken), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.getValidatedRefreshToken(rawToken));
    }

    @Test
    @DisplayName("Should call removal of all user refresh tokens")
    void revokeAllUserTokens_shouldCallRemovalOfUserRefreshTokens() {
        // When
        refreshTokenService.revokeAllUserTokens(testUser);

        // Then
        verify(refreshTokenRepository, times(1)).deleteAllByUser(testUser);
    }
}