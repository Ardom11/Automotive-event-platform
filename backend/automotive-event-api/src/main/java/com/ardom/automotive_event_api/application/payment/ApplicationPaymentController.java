package com.ardom.automotive_event_api.application.payment;

import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Manage event applications for the authenticated user")
@SecurityRequirement(name = "BearerAuth")
public class ApplicationPaymentController {

    private final ApplicationPaymentService service;

    @Operation(
            summary = "Initiate payment for an approved application",
            description = """
                    Creates a Stripe Checkout session for an application in `APPROVED_WAITING_PAYMENT` status
                    and returns the session URL to redirect the user to.
                    
                    **Business rules:**
                    - Application must belong to the authenticated user
                    - Application must be in `APPROVED_WAITING_PAYMENT` status
                    - A checkout session cannot be initiated if one is already in progress
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout session created — redirect the user to the returned URL",
                    content = @Content(schema = @Schema(implementation = CheckoutResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found or belongs to a different user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A checkout session for this application is already in progress",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Stripe API error — upstream payment provider failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/payment")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckoutResponse> payForApplication(
            @Parameter(description = "ID of the application to pay for", example = "42")
            @PathVariable Long id,
            Authentication authentication
    ) throws StripeException {
        return ResponseEntity.ok(service.initiateCheckout(authentication, id));
    }
}
