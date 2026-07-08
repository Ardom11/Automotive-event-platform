package com.ardom.automotive_event_api.application.payment;

import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.user.User;
import com.stripe.exception.StripeException;
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
public class ApplicationPaymentController {
    private final ApplicationPaymentService service;

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckoutResponse> payForApplication(@PathVariable Long id,
                                                              Authentication authentication) throws StripeException {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(service.initiateCheckout(id, user));
    }
}
