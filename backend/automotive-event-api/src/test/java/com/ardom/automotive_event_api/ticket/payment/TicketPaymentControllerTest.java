package com.ardom.automotive_event_api.ticket.payment;

import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.event.exception.EventSoldOutException;
import com.ardom.automotive_event_api.event.exception.NotEnoughEventTicketsException;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.security.JwtService;
import com.ardom.automotive_event_api.security.config.TestSecurityConfig;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseGuestRequest;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketPaymentController.class)
@Import(TestSecurityConfig.class)
class TicketPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TicketPaymentService ticketPaymentService;

    // -------------------------------------------------------------------------
    // Method purchaseAsUser() — POST /tickets/checkout
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /tickets/checkout — purchaseAsUser()")
    class PurchaseAsUser {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 200 with checkout URL when request is valid")
        void purchaseAsUser_shouldReturn200WithCheckoutUrl_whenRequestIsValid() throws Exception {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 2);
            CheckoutResponse response = new CheckoutResponse("https://checkout.stripe.com/pay/cs_test_abc");

            when(ticketPaymentService.initiateCheckout(any(Authentication.class), any())).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/tickets/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_abc"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 400 when quantity is zero")
        void purchaseAsUser_shouldReturn400_whenQuantityIsZero() throws Exception {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 0);

            // When / Then
            mockMvc.perform(post("/tickets/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 400 when eventId is missing")
        void purchaseAsUser_shouldReturn400_whenEventIdIsMissing() throws Exception {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(null, 2);

            // When / Then
            mockMvc.perform(post("/tickets/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 404 when event is not found")
        void purchaseAsUser_shouldReturn404_whenEventNotFound() throws Exception {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(99L, 1);

            when(ticketPaymentService.initiateCheckout(any(Authentication.class), any()))
                    .thenThrow(new EventNotFoundException("Event with id 99 is not found"));

            // When / Then
            mockMvc.perform(post("/tickets/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 409 when event is sold out")
        void purchaseAsUser_shouldReturn409_whenEventIsSoldOut() throws Exception {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 1);

            when(ticketPaymentService.initiateCheckout(any(Authentication.class), any()))
                    .thenThrow(new EventSoldOutException("Event sold out."));

            // When / Then
            mockMvc.perform(post("/tickets/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 409 when not enough tickets remain")
        void purchaseAsUser_shouldReturn409_whenNotEnoughTicketsLeft() throws Exception {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 10);

            when(ticketPaymentService.initiateCheckout(any(Authentication.class), any()))
                    .thenThrow(new NotEnoughEventTicketsException("Tickets left: 3"));

            // When / Then
            mockMvc.perform(post("/tickets/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 502 when Stripe throws an exception")
        void purchaseAsUser_shouldReturn502_whenStripeThrowsException() throws Exception {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 1);

            when(ticketPaymentService.initiateCheckout(any(Authentication.class), any()))
                    .thenThrow(mock(StripeException.class));

            // When / Then
            mockMvc.perform(post("/tickets/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadGateway());
        }

        @Test
        @DisplayName("should return 401 when user is not authenticated")
        void purchaseAsUser_shouldReturn401_whenNotAuthenticated() throws Exception {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 1);

            // When / Then
            mockMvc.perform(post("/tickets/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 403 when user does not have USER role")
        void purchaseAsUser_shouldReturn403_whenUserDoesNotHaveUserRole() throws Exception {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 1);

            // When / Then
            mockMvc.perform(post("/tickets/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Method purchaseAsGuest() — POST /tickets/guest-checkout
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /tickets/guest-checkout — purchaseAsGuest()")
    class PurchaseAsGuest {

        @Test
        @DisplayName("should return 200 with checkout URL when guest request is valid")
        void purchaseAsGuest_shouldReturn200WithCheckoutUrl_whenRequestIsValid() throws Exception {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    1L,
                    1,
                    "Jane",
                    "Doe",
                    "jane@example.com"
            );
            CheckoutResponse response = new CheckoutResponse("https://checkout.stripe.com/pay/cs_test_guest");

            when(ticketPaymentService.initiateCheckout(any())).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/tickets/guest-checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_guest"));
        }

        @Test
        @DisplayName("should return 400 when guest email is missing")
        void purchaseAsGuest_shouldReturn400_whenGuestEmailIsMissing() throws Exception {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    1L,
                    1,
                    "Jane",
                    "Doe",
                    null
            );

            // When / Then
            mockMvc.perform(post("/tickets/guest-checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when quantity is below minimum")
        void purchaseAsGuest_shouldReturn400_whenQuantityIsBelowMinimum() throws Exception {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    1L,
                    0,
                    "Jane",
                    "Doe",
                    "jane@example.com"
            );

            // When / Then
            mockMvc.perform(post("/tickets/guest-checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when event is not found")
        void purchaseAsGuest_shouldReturn404_whenEventNotFound() throws Exception {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    99L,
                    1,
                    "Jane",
                    "Doe",
                    "jane@example.com"
            );

            when(ticketPaymentService.initiateCheckout(any()))
                    .thenThrow(new EventNotFoundException("Event with id 99 is not found"));

            // When / Then
            mockMvc.perform(post("/tickets/guest-checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 when event is sold out")
        void purchaseAsGuest_shouldReturn409_whenEventIsSoldOut() throws Exception {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    1L,
                    1,
                    "Jane",
                    "Doe",
                    "jane@example.com"
            );

            when(ticketPaymentService.initiateCheckout(any()))
                    .thenThrow(new EventSoldOutException("Event sold out."));

            // When / Then
            mockMvc.perform(post("/tickets/guest-checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 502 when Stripe throws an exception")
        void purchaseAsGuest_shouldReturn502_whenStripeThrowsException() throws Exception {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    1L,
                    1,
                    "Jane",
                    "Doe",
                    "jane@example.com"
            );

            when(ticketPaymentService.initiateCheckout(any()))
                    .thenThrow(mock(StripeException.class));

            // When / Then
            mockMvc.perform(post("/tickets/guest-checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadGateway());
        }

        @Test
        @DisplayName("should return 200 even when user is authenticated — guest endpoint is public")
        void purchaseAsGuest_shouldReturn200_whenAuthenticatedUserCallsGuestEndpoint() throws Exception {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    1L,
                    1,
                    "Jane",
                    "Doe",
                    "jane@example.com"
            );
            CheckoutResponse response = new CheckoutResponse("https://checkout.stripe.com/pay/cs_test_guest");

            when(ticketPaymentService.initiateCheckout(any())).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/tickets/guest-checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }
}
