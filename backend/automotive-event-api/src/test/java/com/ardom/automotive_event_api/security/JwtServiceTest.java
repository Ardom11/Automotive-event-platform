package com.ardom.automotive_event_api.auth;

import com.ardom.automotive_event_api.user.Role;
import com.ardom.automotive_event_api.user.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private User testUser;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secret",
                Base64.getEncoder().encodeToString("mySuperSecretKeyForTestingPurposes1234567890AbcXyz".getBytes()));
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);

        testUser = User.builder()
                .id(1L)
                .email("test@mail.com")
                .role(Role.USER)
                .build();
    }


    @Test
    @DisplayName("Should generate valid JWT token with 3 parts")
    void generateToken() {
        //Given & When
        String token = jwtService.generateToken(testUser);

        //Then
        assertFalse(token.isBlank());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Should return claims which contains user details")
    void extractClaims() {
        //Given
        String token = jwtService.generateToken(testUser);

        //When
        Claims claims = jwtService.extractClaims(token);

        //Then
        assertEquals(testUser.getRole().toString(), claims.get("role"));
        assertEquals(testUser.getEmail(), claims.getSubject());
        assertEquals(
                testUser.getId(),
                ((Number) claims.get("id")).longValue());
    }

    @Test
    @DisplayName("Should extract email from token")
    void extractEmail_shouldReturnUserEmail() {
        String token = jwtService.generateToken(testUser);
        assertEquals(testUser.getEmail(), jwtService.extractEmail(token));
    }

    @Nested
    @DisplayName("Method isTokenValid()")
    class isTokenValidTests {

        @Test
        @DisplayName("Should return true if email that token contains matches user and is not expired")
        void isTokenValid_shouldReturnTrue_whenEmailIsMatchUserAndIsNotExpired() {
            //Given
            String token = jwtService.generateToken(testUser);

            //When & Then
            assertTrue(jwtService.isTokenValid(token, testUser));
        }

        @Test
        @DisplayName("Should return false when emails in token and user passed are different")
        void isTokenValid_shouldReturnFalse_whenEmailDiffers() {
            //Given
            String token = jwtService.generateToken(testUser);
            User fakeUser = User.builder().email("fake@mail.com").build();

            //When & Then
            assertFalse(jwtService.isTokenValid(token, fakeUser));
        }

        @Test
        @DisplayName("Should return false when token is already expired")
        void isTokenValid_shouldReturnFalse_whenIsExpired() throws InterruptedException {
            //Given
            ReflectionTestUtils.setField(jwtService, "expiration", 1L);
            String token = jwtService.generateToken(testUser);

            Thread.sleep(5);

            //When & Then
            assertFalse(jwtService.isTokenValid(token, testUser));
        }

        @Test
        @DisplayName("Should return false for a malformed token string")
        void isTokenValid_shouldReturnFalse_whenTokenIsMalformed() {
            assertFalse(jwtService.isTokenValid("this.is.garbage", testUser));
        }
    }
}