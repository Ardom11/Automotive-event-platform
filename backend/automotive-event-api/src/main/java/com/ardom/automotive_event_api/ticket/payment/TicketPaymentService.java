package com.ardom.automotive_event_api.ticket.payment;

import com.ardom.automotive_event_api.event.Event;
import com.ardom.automotive_event_api.event.EventRepository;
import com.ardom.automotive_event_api.event.EventStatus;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.event.exception.EventSoldOutException;
import com.ardom.automotive_event_api.event.exception.NotEnoughEventTicketsException;
import com.ardom.automotive_event_api.payment.PaymentStatus;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.ticket.TicketRepository;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseGuestRequest;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseRequest;
import com.ardom.automotive_event_api.user.User;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TicketPaymentService {
    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CheckoutResponse initiateCheckout(TicketPurchaseRequest request, User user) throws StripeException {
        Event event = getValidEvent(request.eventId(), request.quantity());

        TicketPayment payment = TicketPayment.builder()
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
                .setSuccessUrl("https://localhost:8080/events")
                .setCancelUrl(String.format("https://localhost:8080/events/%s", event.getId()))
                .putMetadata("type", "TICKET")
                .putMetadata("ticketPaymentId", paymentId.toString())
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
