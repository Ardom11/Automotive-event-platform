package com.ardom.automotive_event_api.auth;

import com.ardom.automotive_event_api.auth.dto.request.GoogleLoginRequest;
import com.ardom.automotive_event_api.auth.dto.request.LoginRequest;
import com.ardom.automotive_event_api.auth.dto.request.RegisterRequest;
import com.ardom.automotive_event_api.auth.dto.response.AuthResponse;
import com.ardom.automotive_event_api.auth.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    @Value("${application.security.cookie.secure}")
    private boolean isSecure;

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest registerRequest,
                                                  HttpServletResponse response) {

        AuthResponse authResponse = authService.register(registerRequest);

        setRefreshTokenCookie(authResponse.refreshToken(), response);

        return ResponseEntity.status(HttpStatus.CREATED).body(new TokenResponse(authResponse.accessToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                               HttpServletResponse response) {

        AuthResponse authResponse = authService.login(loginRequest);

        setRefreshTokenCookie(authResponse.refreshToken(), response);

        return ResponseEntity.ok(new TokenResponse(authResponse.accessToken()));
    }

    @PostMapping("/google")
    public ResponseEntity<TokenResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request,
                                                     HttpServletResponse response){
        AuthResponse authResponse = authService.googleLogin(request.googleToken());

        setRefreshTokenCookie(authResponse.refreshToken(), response);

        return ResponseEntity.ok(new TokenResponse(authResponse.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refreshToken") String refreshToken,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.refreshToken(refreshToken);

        setRefreshTokenCookie(authResponse.refreshToken(), response);

        return ResponseEntity.ok(new TokenResponse(authResponse.accessToken()));
    }

    private void setRefreshTokenCookie(String token,
                                       HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
