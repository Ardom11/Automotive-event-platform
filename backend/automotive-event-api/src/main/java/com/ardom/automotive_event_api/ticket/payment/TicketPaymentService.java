package com.ardom.automotive_event_api.ticket.payment;

import com.ardom.automotive_event_api.application.exception.TicketPaymentNotFoundException;
import com.ardom.automotive_event_api.common.email.EmailService;
import com.ardom.automotive_event_api.event.Event;
import com.ardom.automotive_event_api.event.EventRepository;
import com.ardom.automotive_event_api.event.EventStatus;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.event.exception.EventSoldOutException;
import com.ardom.automotive_event_api.event.exception.NotEnoughEventTicketsException;
import com.ardom.automotive_event_api.payment.PaymentStatus;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.ticket.Ticket;
import com.ardom.automotive_event_api.ticket.TicketRepository;
import com.ardom.automotive_event_api.ticket.TicketStatus;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseGuestRequest;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseRequest;
import com.ardom.automotive_event_api.ticket.util.TicketCodeGenerator;
import com.ardom.automotive_event_api.user.User;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketPaymentService {
    @Value("${application.base-url}")
    private String baseUrl;

    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final EmailService emailService;

    @Transactional
    public CheckoutResponse initiateCheckout(Authentication authentication, TicketPurchaseRequest request) throws StripeException {
        User user = (User) authentication.getPrincipal();

        Event event = getValidEvent(request.eventId(), request.quantity());

        TicketPayment payment = TicketPayment.builder()
                .event(event)
                .user(user)
                .quantity(request.quantity())
                .amountPaid(event.getTicketPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .status(PaymentStatus.PENDING)
                .build();

        payment = ticketPaymentRepository.save(payment);

        Session session = createSession(event, payment.getId(), user.getEmail(), request.quantity());

        payment.setStripePaymentId(session.getId());
        ticketPaymentRepository.save(payment);

        return new CheckoutResponse(session.getUrl());
    }

    @Transactional
    public CheckoutResponse initiateCheckout(TicketPurchaseGuestRequest request) throws StripeException {
        Event event = getValidEvent(request.eventId(), request.quantity());

        TicketPayment payment = TicketPayment.builder()
                .event(event)
                .guestName(request.guestName())
                .guestSurname(request.guestSurname())
                .guestEmail(request.guestEmail())
                .quantity(request.quantity())
                .amountPaid(event.getTicketPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .status(PaymentStatus.PENDING)
                .build();

        payment = ticketPaymentRepository.save(payment);

        Session session = createSession(event, payment.getId(), request.guestEmail(), request.quantity());

        payment.setStripePaymentId(session.getId());
        ticketPaymentRepository.save(payment);

        return new CheckoutResponse(session.getUrl());
    }

    @Transactional
    public void handleSuccessfulCheckout(Long ticketPaymentId) {
        TicketPayment payment = ticketPaymentRepository.findById(ticketPaymentId)
                .orElseThrow(() -> new TicketPaymentNotFoundException("Ticket payment with id " + ticketPaymentId + " not found"));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(LocalDateTime.now());

        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < payment.getQuantity(); i++) {
            Ticket ticket = Ticket.builder()
                    .payment(payment)
                    .event(payment.getEvent())
                    .user(payment.getUser())
                    .guestEmail(payment.getGuestEmail())
                    .code(TicketCodeGenerator.generate())
                    .status(TicketStatus.ACTIVE)
                    .price(payment.getEvent().getTicketPrice())
                    .build();
            tickets.add(ticket);
        }

        ticketRepository.saveAll(tickets);
        emailService.sendTicket(
                tickets,
                payment.getUser() != null ? payment.getUser().getEmail() : payment.getGuestEmail(),
                payment.getEvent().getName()
        );
    }

    @Transactional
    public void handleFailedCheckout(Long referenceId) {
        TicketPayment payment = ticketPaymentRepository.findById(referenceId)
                .orElseThrow(() -> new TicketPaymentNotFoundException("Ticket payment with id " + referenceId + " is not found"));

        if (!payment.getStatus().equals(PaymentStatus.FAILED)) {
            payment.setStatus(PaymentStatus.FAILED);
        }
    }

    private Event getValidEvent(Long eventId, int amountToPurchase) {
        Event event = eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED)
                .orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " is not found"));

        int alreadySold = ticketRepository.countByEventId(eventId);
        if (alreadySold == event.getTicketsCapacity()) {
            throw new EventSoldOutException("Event sold out.");
        }

        if (alreadySold + amountToPurchase > event.getTicketsCapacity()) {
            throw new NotEnoughEventTicketsException("There is no enough tickets to proceed." +
                    " Tickets left: " + (event.getTicketsCapacity() - alreadySold));
        }

        return event;
    }

    private Session createSession(Event event, Long paymentId, String email, int quantity) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/events")
                .setCancelUrl(baseUrl + "/events/" + event.getId())
                .putMetadata("type", "TICKET")
                .putMetadata("referenceId", paymentId.toString())
                .setCustomerEmail(email)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity((long) quantity)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(event.getTicketPrice().multiply(BigDecimal.valueOf(100)).longValueExact())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Ticket — " + event.getName())
                                        .build())
                                .build())
                        .build())
                .build();

        return Session.create(params);
    }
}
