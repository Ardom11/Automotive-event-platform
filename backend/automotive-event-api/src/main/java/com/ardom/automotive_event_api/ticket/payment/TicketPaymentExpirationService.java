package com.ardom.automotive_event_api.ticket.payment;

import com.ardom.automotive_event_api.payment.PaymentStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketPaymentExpirationService {

    private final TicketPaymentRepository ticketPaymentRepository;

    @Transactional
    public void expireNotFinishedPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);

        List<TicketPayment> toExpire = ticketPaymentRepository
                .findAllByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cutoff);

        toExpire.forEach(payment -> payment.setStatus(PaymentStatus.EXPIRED));
    }
}
