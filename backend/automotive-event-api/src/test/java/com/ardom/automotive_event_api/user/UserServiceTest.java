package com.ardom.automotive_event_api.user;

import com.ardom.automotive_event_api.user.dto.request.UpdateProfileRequest;
import com.ardom.automotive_event_api.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;

    private UserService userService;
    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);

        testUser = User.builder()
                .id(1L)
                .name("Name")
                .surname("Surname")
                .email("test@gmail.com")
                .password("hashedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now().minusDays(1))
                .modifiedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("Method getCurrentUser()")
    void getCurrentUser() {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);

        // When
        User currentUser = userService.getCurrentUser(authentication);

        // Then
        assertEquals(testUser.getId(), currentUser.getId());
        assertEquals(testUser.getEmail(), currentUser.getEmail());
    }


    @Nested
    @DisplayName("Method updateProfile()")
    class UpdateProfileTest {

        @Test
        @DisplayName("Should change user name, surname and modifiedAt")
        void updateProfile_shouldChangeUserNameSurnameModifiedAt() {
            // Given
            LocalDateTime time = testUser.getModifiedAt();
            UpdateProfileRequest request = new UpdateProfileRequest("NewName", "NewSurname");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            userService.updateProfile(1L, request);

            //Then
            assertNotEquals("Name", testUser.getName());
            assertNotEquals("Surname", testUser.getSurname());
            assertNotEquals(time, testUser.getModifiedAt());
        }

        @Test
        @DisplayName("Should change user name & leave surname unchanged")
        void updateProfile_shouldChangeUserNameAndNotSurname(){
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest("NewName", null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            userService.updateProfile(1L, request);

            //Then
            assertNotEquals("Name", testUser.getName());
            assertEquals("Surname", testUser.getSurname());
            assertNotNull(testUser.getSurname());
        }

        @Test
        @DisplayName("Should change user surname & leave name unchanged")
        void updateProfile_shouldChangeUserSurnameAndNotName(){
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest(null, "NewSurname");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            userService.updateProfile(1L, request);

            //Then
            assertEquals("Name", testUser.getName());
            assertNotNull(testUser.getName());
            assertNotEquals("Surname", testUser.getSurname());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException")
        void updateProfile_shouldThrowUserNotFoundException(){
            when(userRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(UserNotFoundException.class,
                    () -> userService.updateProfile(1L, new UpdateProfileRequest(null, null)));
        }
    }
}