package com.ardom.automotive_event_api.auth;

import com.ardom.automotive_event_api.auth.dto.request.LoginRequest;
import com.ardom.automotive_event_api.auth.dto.request.RegisterRequest;
import com.ardom.automotive_event_api.auth.dto.response.AuthResponse;
import com.ardom.automotive_event_api.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    JwtService jwtService;

    private static final String ACCESS_TOKEN = "access.token.value";
    private static final String REFRESH_TOKEN = "refresh-token-uuid";
    private static final AuthResponse AUTH_RESPONSE = new AuthResponse(ACCESS_TOKEN, REFRESH_TOKEN);

    // -------------------------------------------------------------------------
    // POST /auth/register
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        private RegisterRequest validRequest() {
            return new RegisterRequest("John", "Doe", "john@gmail.com", "password123");
        }

        @Test
        @DisplayName("Should return 201 with access token and set refreshToken cookie on valid request")
        void register_shouldReturn201WithTokenAndCookie() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(AUTH_RESPONSE);

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                    .andExpect(header().string("Set-Cookie",
                            org.hamcrest.Matchers.containsString("refreshToken=" + REFRESH_TOKEN)))
                    .andExpect(header().string("Set-Cookie",
                            org.hamcrest.Matchers.containsString("HttpOnly")))
                    .andExpect(header().string("Set-Cookie",
                            org.hamcrest.Matchers.containsString("Path=/")));
        }

        @Test
        @DisplayName("Should return 400 when request body is missing required fields")
        void register_shouldReturn400_whenBodyIsInvalid() throws Exception {
            RegisterRequest invalid = new RegisterRequest("", "", "not-an-email", "");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Should return 400 when request body is empty")
        void register_shouldReturn400_whenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    // -------------------------------------------------------------------------
    // POST /auth/login
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        private LoginRequest validRequest() {
            return new LoginRequest("john@gmail.com", "password123");
        }

        @Test
        @DisplayName("Should return 200 with access token and set refreshToken cookie on valid credentials")
        void login_shouldReturn200WithTokenAndCookie() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(AUTH_RESPONSE);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                    .andExpect(header().string("Set-Cookie",
                            org.hamcrest.Matchers.containsString("refreshToken=" + REFRESH_TOKEN)));
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void login_shouldReturn400_whenEmailIsInvalid() throws Exception {
            LoginRequest invalid = new LoginRequest("not-an-email", "password123");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Should return 400 when body is empty")
        void login_shouldReturn400_whenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    // -------------------------------------------------------------------------
    // POST /auth/refresh
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        @Test
        @DisplayName("Should return 200 with new access token and rotate refreshToken cookie")
        void refresh_shouldReturn200WithNewTokenAndRotateCookie() throws Exception {
            AuthResponse rotated = new AuthResponse("new.access.token", "new-refresh-uuid");
            when(authService.refreshToken(REFRESH_TOKEN)).thenReturn(rotated);

            mockMvc.perform(post("/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", REFRESH_TOKEN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                    .andExpect(header().string("Set-Cookie",
                            org.hamcrest.Matchers.containsString("refreshToken=new-refresh-uuid")));
        }

        @Test
        @DisplayName("Should return 400 when refreshToken cookie is absent")
        void refresh_shouldReturn400_whenCookieIsMissing() throws Exception {
            mockMvc.perform(post("/auth/refresh"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }
}