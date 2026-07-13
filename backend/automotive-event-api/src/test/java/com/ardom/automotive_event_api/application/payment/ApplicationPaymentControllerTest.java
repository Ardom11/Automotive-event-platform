package com.ardom.automotive_event_api.application.payment;

import com.ardom.automotive_event_api.application.exception.ApplicationNotFoundException;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.payment.exception.PaymentAlreadyInitiatedException;
import com.ardom.automotive_event_api.security.JwtService;
import com.ardom.automotive_event_api.security.config.TestSecurityConfig;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationPaymentController.class)
@Import(TestSecurityConfig.class)
class ApplicationPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private ApplicationPaymentService applicationPaymentService;

    // -------------------------------------------------------------------------
    // Method payForApplication() — POST /applications/{id}/payment
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /applications/{id}/payment — payForApplication()")
    class PayForApplication {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 200 with checkout URL when application is valid")
        void payForApplication_shouldReturn200WithCheckoutUrl_whenApplicationIsValid() throws Exception {
            // Given
            CheckoutResponse response = new CheckoutResponse("https://checkout.stripe.com/pay/cs_test_app");

            when(applicationPaymentService.initiateCheckout(any(Authentication.class), eq(5L))).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/applications/5/payment")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.checkoutUrl")
                            .value("https://checkout.stripe.com/pay/cs_test_app"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 404 when application is not found or does not belong to user")
        void payForApplication_shouldReturn404_whenApplicationNotFound() throws Exception {
            // Given
            when(applicationPaymentService.initiateCheckout(any(Authentication.class), eq(99L)))
                    .thenThrow(new ApplicationNotFoundException("Application with id 99 not found"));

            // When / Then
            mockMvc.perform(post("/applications/99/payment")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 409 when payment is already initiated for this application")
        void payForApplication_shouldReturn409_whenPaymentAlreadyInitiated() throws Exception {
            // Given
            when(applicationPaymentService.initiateCheckout(any(Authentication.class), eq(5L)))
                    .thenThrow(new PaymentAlreadyInitiatedException("Payment already initiated for this application."));

            // When / Then
            mockMvc.perform(post("/applications/5/payment")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 502 when Stripe throws an exception")
        void payForApplication_shouldReturn502_whenStripeThrowsException() throws Exception {
            // Given
            when(applicationPaymentService.initiateCheckout(any(Authentication.class), eq(5L)))
                    .thenThrow(mock(StripeException.class));

            // When / Then
            mockMvc.perform(post("/applications/5/payment")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadGateway());
        }

        @Test
        @DisplayName("should return 401 when user is not authenticated")
        void payForApplication_shouldReturn401_whenNotAuthenticated() throws Exception {
            // When / Then
            mockMvc.perform(post("/applications/5/payment")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 403 when user does not have USER role")
        void payForApplication_shouldReturn403_whenUserDoesNotHaveUserRole() throws Exception {
            // When / Then
            mockMvc.perform(post("/applications/5/payment")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}
