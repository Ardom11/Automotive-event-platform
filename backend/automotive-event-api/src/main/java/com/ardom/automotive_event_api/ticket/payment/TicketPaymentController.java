package com.ardom.automotive_event_api.ticket.payment;

import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseGuestRequest;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseRequest;
import com.stripe.exception.StripeException;
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
public class TicketPaymentController {
    private final TicketPaymentService service;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckoutResponse> purchaseAsUser(Authentication authentication,
                                                           @Valid @RequestBody TicketPurchaseRequest request) throws StripeException {
        return ResponseEntity.ok(service.initiateCheckout(authentication, request));
    }

    @PostMapping("/guest-checkout")
    public ResponseEntity<CheckoutResponse> purchaseAsGuest(
            @Valid @RequestBody TicketPurchaseGuestRequest request) throws StripeException {
        return ResponseEntity.ok(service.initiateCheckout(request));
    }
}
