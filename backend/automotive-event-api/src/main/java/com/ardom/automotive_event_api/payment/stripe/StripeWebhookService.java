package com.ardom.automotive_event_api.payment.stripe;

import com.ardom.automotive_event_api.application.payment.ApplicationPaymentService;
import com.ardom.automotive_event_api.payment.exception.WebhookException;
import com.ardom.automotive_event_api.ticket.payment.TicketPaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final TicketPaymentService ticketPaymentService;
    private final ApplicationPaymentService applicationPaymentService;

    public void processEvent(String payload, String sigHeader) {
        Event event = verifyAndConstruct(payload, sigHeader);

        String eventType = event.getType();
        if (!eventType.equals("checkout.session.completed")) {
            log.debug("Ignoring event type: {}", eventType);
            return;
        }

        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));

        String paymentType = session.getMetadata().get("type");
        Long referenceId = Long.valueOf(session.getMetadata().get("referenceId"));
        switch (paymentType) {
            case "TICKET" -> ticketPaymentService.handleSuccessfulCheckout(referenceId);
            case "APPLICATION" -> applicationPaymentService.handleSuccessfulCheckout(referenceId);
            default -> log.warn("Unknown payment type: {}", paymentType);
        }
    }

    private Event verifyAndConstruct(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new WebhookException("Invalid Stripe signature");
        }
    }
}
