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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.email())) {
            log.warn("User with email {} already exists", maskEmail(registerRequest.email()));
            throw new UserAlreadyExistsException("Email already taken");
        }
        User user = authMapper.toEntity(registerRequest);

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);
        log.info("User {} registered successfully", maskEmail(registerRequest.email()));

        return new AuthResponse(jwtService.generateToken(user),
                refreshTokenService.createRefreshToken(user));
    }

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            User user = (User) authentication.getPrincipal();

            refreshTokenService.revokeAllUserTokens(user);

            log.info("User {} logged in successfully", maskEmail(loginRequest.email()));
            return new AuthResponse(jwtService.generateToken(user),
                    refreshTokenService.createRefreshToken(user));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid login or password");
        }
    }

    public AuthResponse googleLogin(String googleToken) {
        GoogleIdToken.Payload payload = googleTokenVerifierService.verify(googleToken);

        String email = payload.getEmail();
        String name = (String) payload.get("given_name");
        String surname = (String) payload.get("family_name");
        String googleId = payload.getSubject();

        User user = userRepository.findByEmail(email)
                .map(existing -> {
                    if (existing.getGoogleId() == null) {
                        throw new UserAlreadyExistsException(
                                "An account with this email already exists. Please login with your password."
                        );
                    }
                    return existing;
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .surname(surname != null ? surname : "")
                                .googleId(googleId)
                                .role(Role.USER)
                                .build()
                ));

        return new AuthResponse(jwtService.generateToken(user),
                refreshTokenService.createRefreshToken(user));
    }

    @Transactional
    public AuthResponse refreshToken(String token) {
        User user = refreshTokenService.getValidatedRefreshToken(token)
                .getUser();

        refreshTokenService.revokeAllUserTokens(user);
        log.info("All refresh tokens revoked for user {}", maskEmail(user.getEmail()));

        log.info("User {} refreshed access token successfully", maskEmail(user.getEmail()));
        return new AuthResponse(jwtService.generateToken(user),
                refreshTokenService.createRefreshToken(user));
    }

    private String maskEmail(String email) {
        String[] splitted = email.split("@");
        String name = splitted[0];
        if (name.length() <= 1) return email;

        return name.charAt(0) + "*".repeat(name.length() - 1) + "@" + splitted[1];
    }
}