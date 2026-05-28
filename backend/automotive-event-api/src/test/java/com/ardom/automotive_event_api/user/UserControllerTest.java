package com.ardom.automotive_event_api.user;

import com.ardom.automotive_event_api.security.config.TestSecurityConfig;
import com.ardom.automotive_event_api.user.dto.request.UpdateProfileRequest;
import com.ardom.automotive_event_api.user.dto.response.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    private User testUser;
    private UserResponse testUserResponse;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testUser = User.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .email("john.doe@gmail.com")
                .password("hashedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        testUserResponse = new UserResponse(1L, "John", "Doe", "john.doe@gmail.com", Role.USER);

        mockAuth = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
    }

    // -------------------------------------------------------------------------
    // GET /users/me
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("Should return 200 with user data")
        void getCurrentUser_shouldReturn200WithUserData() throws Exception {
            // Given
            when(userService.getCurrentUser(any(Authentication.class))).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            // When / Then
            mockMvc.perform(get("/users/me")
                            .with(authentication(mockAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("John"))
                    .andExpect(jsonPath("$.surname").value("Doe"))
                    .andExpect(jsonPath("$.email").value("john.doe@gmail.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @Disabled("Security not configured yet — re-enable when SecurityFilterChain is in place")
        @DisplayName("Should return 401 when request is unauthenticated")
        void getCurrentUser_shouldReturn401_whenUnauthenticated() throws Exception {
            // When / Then
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /users/me
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /users/me")
    class UpdateProfile {

        @Test
        @DisplayName("Should return 200 with updated user data when both fields are provided")
        void updateProfile_shouldReturn200WithUpdatedData() throws Exception {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith");
            UserResponse updatedResponse = new UserResponse(1L, "Jane", "Smith", "john.doe@gmail.com", Role.USER);
            User updatedUser = User.builder()
                    .id(1L).name("Jane").surname("Smith")
                    .email("john.doe@gmail.com").role(Role.USER).build();

            when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(updatedUser);
            when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);

            // When / Then
            mockMvc.perform(patch("/users/me")
                            .with(authentication(mockAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Jane"))
                    .andExpect(jsonPath("$.surname").value("Smith"));
        }

        @Test
        @DisplayName("Should return 200 when only name is provided")
        void updateProfile_shouldReturn200_whenOnlyNameProvided() throws Exception {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", null);
            UserResponse updatedResponse = new UserResponse(1L, "Jane", "Doe", "john.doe@gmail.com", Role.USER);
            User updatedUser = User.builder()
                    .id(1L).name("Jane").surname("Doe")
                    .email("john.doe@gmail.com").role(Role.USER).build();

            when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(updatedUser);
            when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);

            // When / Then
            mockMvc.perform(patch("/users/me")
                            .with(authentication(mockAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Jane"))
                    .andExpect(jsonPath("$.surname").value("Doe"));
        }

        @Test
        @DisplayName("Should return 200 when only surname is provided")
        void updateProfile_shouldReturn200_whenOnlySurnameProvided() throws Exception {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest(null, "Smith");
            UserResponse updatedResponse = new UserResponse(1L, "John", "Smith", "john.doe@gmail.com", Role.USER);
            User updatedUser = User.builder()
                    .id(1L).name("John").surname("Smith")
                    .email("john.doe@gmail.com").role(Role.USER).build();

            when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(updatedUser);
            when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);

            // When / Then
            mockMvc.perform(patch("/users/me")
                            .with(authentication(mockAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("John"))
                    .andExpect(jsonPath("$.surname").value("Smith"));
        }

        @Test
        @DisplayName("Should return 400 when name is too short")
        void updateProfile_shouldReturn400_whenNameTooShort() throws Exception {
            // Given — "A" is 1 char, violates @Size(min = 2)
            UpdateProfileRequest request = new UpdateProfileRequest("A", "Doe");

            // When / Then
            mockMvc.perform(patch("/users/me")
                            .with(authentication(mockAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when surname is too short")
        void updateProfile_shouldReturn400_whenSurnameTooShort() throws Exception {
            // Given — "D" is 1 char, violates @Size(min = 2)
            UpdateProfileRequest request = new UpdateProfileRequest("John", "D");

            // When / Then
            mockMvc.perform(patch("/users/me")
                            .with(authentication(mockAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when name exceeds max length")
        void updateProfile_shouldReturn400_whenNameTooLong() throws Exception {
            // Given — 51 chars violates @Size(max = 50)
            UpdateProfileRequest request = new UpdateProfileRequest("A".repeat(51), "Doe");

            // When / Then
            mockMvc.perform(patch("/users/me")
                            .with(authentication(mockAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Disabled("Security not configured yet — re-enable when SecurityFilterChain is in place")
        @DisplayName("Should return 401 when request is unauthenticated")
        void updateProfile_shouldReturn401_whenUnauthenticated() throws Exception {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith");

            // When / Then
            mockMvc.perform(patch("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}