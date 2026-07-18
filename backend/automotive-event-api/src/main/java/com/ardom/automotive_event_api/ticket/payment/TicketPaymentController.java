package com.ardom.automotive_event_api.ticket.payment;

import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseGuestRequest;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseRequest;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Buy tickets as guest or authenticated user")
public class TicketPaymentController {

    private final TicketPaymentService service;

    @Operation(
            summary = "Purchase tickets as an authenticated user",
            description = """
                    Creates a Stripe Checkout session for purchasing one or more tickets to a published event.
                    Returns a Stripe-hosted URL to redirect the user to for payment.
                    On success, tickets are generated and emailed to the user's registered address.
                    
                    **Business rules:**
                    - Event must exist and be in `PUBLISHED` status
                    - Requested quantity must not exceed remaining ticket capacity
                    - Total charged: `event.ticketPrice × quantity`
                    """
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout session created — redirect user to the returned URL",
                    content = @Content(schema = @Schema(implementation = CheckoutResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Event not found or not published",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = """
                    One of:
                    - Event is completely sold out
                    - Requested quantity exceeds remaining ticket capacity
                    """,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Stripe API error — upstream payment provider failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckoutResponse> purchaseAsUser(
            Authentication authentication,
            @Valid @RequestBody TicketPurchaseRequest request
    ) throws StripeException {
        return ResponseEntity.ok(service.initiateCheckout(authentication, request));
    }

    @Operation(
            summary = "Purchase tickets as a guest (no account required)",
            description = """
                    Creates a Stripe Checkout session for an unauthenticated guest.
                    Returns a Stripe-hosted URL to redirect the guest to for payment.
                    On success, tickets are generated and emailed to `guestEmail`.
                    No account is created as part of this flow.
                    
                    **Business rules:**
                    - Event must exist and be in `PUBLISHED` status
                    - Requested quantity must not exceed remaining ticket capacity
                    - Total charged: `event.ticketPrice × quantity`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout session created — redirect guest to the returned URL",
                    content = @Content(schema = @Schema(implementation = CheckoutResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body validation failed (blank name, invalid email, etc.)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found or not published",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = """
                    One of:
                    - Event is completely sold out
                    - Requested quantity exceeds remaining ticket capacity
                    """,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Stripe API error — upstream payment provider failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/guest-checkout")
    public ResponseEntity<CheckoutResponse> purchaseAsGuest(
            @Valid @RequestBody TicketPurchaseGuestRequest request
    ) throws StripeException {
        return ResponseEntity.ok(service.initiateCheckout(request));
    }
}
