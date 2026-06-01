package com.ardom.automotive_event_api.auth.refreshtoken;

import com.ardom.automotive_event_api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalseAndExpiresAtAfter(String token, LocalDateTime now);

    void deleteAllByUser(User user);
}
