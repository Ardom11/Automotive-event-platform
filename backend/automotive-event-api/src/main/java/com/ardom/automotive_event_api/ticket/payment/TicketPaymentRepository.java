package com.ardom.automotive_event_api.ticket.payment;

import com.ardom.automotive_event_api.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketPaymentRepository extends JpaRepository<TicketPayment, Long> {
    List<TicketPayment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime createdAtBefore);
}
