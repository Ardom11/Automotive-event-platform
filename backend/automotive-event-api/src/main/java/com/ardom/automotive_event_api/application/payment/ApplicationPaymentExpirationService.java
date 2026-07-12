package com.ardom.automotive_event_api.application.payment;

import com.ardom.automotive_event_api.payment.PaymentStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationPaymentExpirationService {
    private final ApplicationPaymentRepository applicationPaymentRepository;

    @Transactional
    public void expireNotFinishedPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);

        List<ApplicationPayment> toExpire = applicationPaymentRepository
                .findAllByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cutoff);

        toExpire.forEach(payment -> payment.setStatus(PaymentStatus.EXPIRED));
    }
}
