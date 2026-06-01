package com.ardom.automotive_event_api.auth.refreshtoken;

import com.ardom.automotive_event_api.auth.exception.InvalidRefreshTokenException;
import com.ardom.automotive_event_api.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("Refresh token for user created successfully");

        return refreshToken.getToken();
    }

    public RefreshToken getValidatedRefreshToken(String token) {
        return refreshTokenRepository
                .findByTokenAndRevokedFalseAndExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> {
                    log.debug("Refresh token expired or revoked");
                    return new InvalidRefreshTokenException("Refresh token expired or revoked");
                });
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }
}