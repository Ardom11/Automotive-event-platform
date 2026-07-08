package com.ardom.automotive_event_api.ticket.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketPaymentRepository extends JpaRepository<TicketPayment, Long> {
}
