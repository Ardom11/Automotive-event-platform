package com.ardom.automotive_event_api.auth;

import com.ardom.automotive_event_api.auth.dto.request.GoogleLoginRequest;
import com.ardom.automotive_event_api.auth.dto.request.LoginRequest;
import com.ardom.automotive_event_api.auth.dto.request.RegisterRequest;
import com.ardom.automotive_event_api.auth.dto.response.AuthResponse;
import com.ardom.automotive_event_api.auth.dto.response.TokenResponse;
import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Manage registration and login")
public class AuthController {

    @Value("${application.security.cookie.secure}")
    private boolean isSecure;

    private final AuthService authService;

    @Operation(
            summary = "Register a new user",
            description = """
                    Creates a new user and returns access token
                    Sets a cookie with refresh token
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully registered, access token returned",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "409", description = "User with this email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest registerRequest,
                                                  HttpServletResponse response) {

        AuthResponse authResponse = authService.register(registerRequest);

        setRefreshTokenCookie(authResponse.refreshToken(), response);

        return ResponseEntity.status(HttpStatus.CREATED).body(new TokenResponse(authResponse.accessToken()));
    }

    @Operation(
            summary = "Login into account",
            description = """
                    Login into account using email and password
                    Returns access token and initiates refresh token rotation
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in successfully",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad credentials",
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                               HttpServletResponse response) {

        AuthResponse authResponse = authService.login(loginRequest);

        setRefreshTokenCookie(authResponse.refreshToken(), response);

        return ResponseEntity.ok(new TokenResponse(authResponse.accessToken()));
    }

    @Operation(
            summary = "Login with Google",
            description = """
                    Login with Google account through Google OAuth
                    Returns access token
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in successfully",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    @PostMapping("/google")
    public ResponseEntity<TokenResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request,
                                                     HttpServletResponse response) {
        AuthResponse authResponse = authService.googleLogin(request.googleToken());

        setRefreshTokenCookie(authResponse.refreshToken(), response);

        return ResponseEntity.ok(new TokenResponse(authResponse.accessToken()));
    }

    @Operation(
            summary = "Refresh tokens",
            description = """
                    Initiates refresh token rotation and new access token generation
                    Sets new refresh token as cookie and returns access token as response
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rotated successfully",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Expired or revoked refresh token",
                    content = @Content)
    })
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
